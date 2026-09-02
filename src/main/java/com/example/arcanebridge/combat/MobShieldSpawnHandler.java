package com.example.arcanebridge.combat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = MobArchetypes.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MobShieldSpawnHandler {

    private static final String NBT_INITIALIZED = "arcane_shield_initialized";
    private static final Random RNG = new Random();

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        if (target instanceof Player || target instanceof ArmorStand) return;
        if (target instanceof Animal || target instanceof WaterAnimal || target instanceof AmbientCreature) return;

        CompoundTag persistentData = target.getPersistentData();
        if (persistentData.getBoolean(NBT_INITIALIZED)) return;
        persistentData.putBoolean(NBT_INITIALIZED, true);

        if (tryApplyCommandTags(target, persistentData)) return;

        float maxHp = target.getMaxHealth();
        // Ранг 0 (Рядовой / Fodder) — щиты не выдаются
        if (maxHp < 25.0F) return;

        Level level = event.getLevel();
        String stage = getMajruszStage(level);

        int layerCount = 0;

        // Определение ранга и количества слоев
        if (maxHp < 45.0F) {
            // Ветеран (25 - 44 HP)
            float chance = switch (stage) {
                case "MASTER" -> 0.60F;
                case "EXPERT" -> 0.35F;
                default -> 0.10F;
            };
            if (RNG.nextFloat() < chance) layerCount = 1;
        } else if (maxHp < 80.0F) {
            // Элита (45 - 79 HP)
            float chance = switch (stage) {
                case "MASTER" -> 1.00F;
                case "EXPERT" -> 0.80F;
                default -> 0.40F;
            };
            if (RNG.nextFloat() < chance) {
                if (stage.equals("NORMAL")) {
                    layerCount = 1;
                } else if (stage.equals("EXPERT")) {
                    layerCount = (RNG.nextFloat() < 0.25F) ? 2 : 1;
                } else {
                    layerCount = (RNG.nextFloat() < 0.50F) ? 2 : 1;
                }
            }
        } else if (maxHp <= 160.0F) {
            // Мини-босс / Чемпион (80 - 160 HP)
            layerCount = (stage.equals("MASTER") && RNG.nextFloat() < 0.30F) ? 3 : 2;
        } else {
            // Босс / Титан (> 160 HP)
            layerCount = 3;
        }

        if (layerCount <= 0) return;

        // Расчет базового щитового бюджета
        float baseBudget;
        if (maxHp < 100.0F) {
            baseBudget = maxHp * 0.32F;
        } else if (maxHp <= 300.0F) {
            baseBudget = maxHp * 0.36F;
        } else {
            baseBudget = maxHp * 0.40F; // Для 1000 HP базовый бюджет = 400 HP
        }

        // Дробление и масштабирование пула щита
        float layerHp;
        if (layerCount == 1) {
            layerHp = (float) Math.ceil(baseBudget);
        } else if (layerCount == 2) {
            float totalPool = baseBudget * 1.25F; // 500 при 1000 HP
            layerHp = (float) Math.ceil(totalPool / 2.0F); // по 250 HP
        } else {
            float totalPool = baseBudget * 1.50F; // 600 при 1000 HP
            layerHp = (float) Math.ceil(totalPool / 3.0F); // по 200 HP
        }

        // Сборка тактической цепочки уникальных слоев (без дублей)
        List<String> archetypes = buildUniqueArchetypeChain(target, layerCount);
        ListTag layersList = new ListTag();

        for (String arch : archetypes) {
            CompoundTag layer = new CompoundTag();
            layer.putString("Type", arch);
            layer.putFloat("HP", layerHp);
            layer.putFloat("MaxHP", layerHp);
            layersList.add(layer);
        }

                persistentData.put(MobArchetypes.NBT_SHIELD_LAYERS, layersList);
        persistentData.putInt(MobArchetypes.NBT_CURRENT_LAYER_INDEX, 0);
        persistentData.putBoolean(MobArchetypes.NBT_ALL_SHIELDS_BROKEN, false);

        // Мгновенная синхронизация созданных щитов моба с клиентами
        ArchetypeDamageHandler.syncShieldToTracking(target);
    }

    /**
     * Генерация цепочки архетипов без дубликатов с правильным порядком слоев
     */
    private static List<String> buildUniqueArchetypeChain(LivingEntity entity, int count) {
        List<String> pool = new ArrayList<>();
        pool.add("ARMORED");
        pool.add("ETHEREAL");
        pool.add("BIO");

        String priority = getPriorityArchetype(entity);
        List<String> result = new ArrayList<>();

        if (count == 1) {
            result.add(priority);
            return result;
        }

        if (count == 2) {
            pool.remove(priority);
            Collections.shuffle(pool, RNG);
            String secondary = pool.get(0);

            // Тактический порядок: Механика глубже, Эфир/Био на поверхности
            if (priority.equals("ARMORED")) {
                result.add(secondary); // Внешний
                result.add(priority);  // Внутренний
            } else {
                result.add(priority);  // Внешний
                result.add(secondary); // Внутренний
            }
            return result;
        }

        // 3 слоя: присутствуют все три типа в тактическом порядке
        if (priority.equals("ARMORED")) {
            result.add("ETHEREAL"); // Внешний
            result.add("BIO");      // Средний
            result.add("ARMORED");  // Внутренний
        } else if (priority.equals("ETHEREAL")) {
            result.add("ETHEREAL");
            result.add("BIO");
            result.add("ARMORED");
        } else {
            result.add("BIO");
            result.add("ETHEREAL");
            result.add("ARMORED");
        }
        return result;
    }

    private static String getPriorityArchetype(LivingEntity entity) {
        if (hasHeavyArmor(entity)) return "ARMORED";
        if (isMagicOrAirEntity(entity)) return "ETHEREAL";
        if (isBioEntity(entity)) return "BIO";

        int roll = RNG.nextInt(3);
        return roll == 0 ? "ARMORED" : (roll == 1 ? "ETHEREAL" : "BIO");
    }

    private static boolean hasHeavyArmor(LivingEntity entity) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.getItem() instanceof ArmorItem armor) {
                var mat = armor.getMaterial();
                if (mat == ArmorMaterials.IRON || mat == ArmorMaterials.DIAMOND ||
                    mat == ArmorMaterials.NETHERITE || mat == ArmorMaterials.CHAIN) return true;
                String name = mat.getName().toLowerCase();
                if (name.contains("brass") || name.contains("steel") || name.contains("enderium")) return true;
            }
        }
        return false;
    }

    private static boolean isMagicOrAirEntity(LivingEntity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String path = id.getPath().toLowerCase();
        String namespace = id.getNamespace().toLowerCase();

        return namespace.equals("ars_nouveau") || namespace.equals("hexcasting") ||
               path.contains("witch") || path.contains("evoker") || path.contains("illusioner") ||
               path.contains("enderman") || path.contains("phantom") || path.contains("blaze") ||
               path.contains("ghast") || path.contains("shulker") || path.contains("stalker");
    }

    private static boolean isBioEntity(LivingEntity entity) {
        if (entity.getMobType() == MobType.ARTHROPOD) return true;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String path = id.getPath().toLowerCase();

        return path.contains("husk") || path.contains("slime") || path.contains("spider") ||
               path.contains("silverfish") || path.contains("drowned") || path.contains("mutant");
    }

    private static String getMajruszStage(Level level) {
        try {
            Class<?> helperClass = Class.forName("com.majruszsdifficulty.gamestage.GameStageHelper");
            for (Method m : helperClass.getMethods()) {
                if (m.getParameterCount() == 0 && (m.getName().equals("getGameStage") || m.getName().equals("getCurrentStage"))) {
                    Object stageObj = m.invoke(null);
                    if (stageObj != null) return stageObj.toString().toUpperCase();
                }
            }
        } catch (Throwable ignored) {}

        if (level.dimension() == Level.END) return "MASTER";
        if (level.dimension() == Level.NETHER) return "EXPERT";
        return "NORMAL";
    }

    private static boolean tryApplyCommandTags(LivingEntity target, CompoundTag persistentData) {
        String manualType = null;
        if (target.getTags().contains("arcane_armored")) manualType = "ARMORED";
        else if (target.getTags().contains("arcane_ethereal")) manualType = "ETHEREAL";
        else if (target.getTags().contains("arcane_bio")) manualType = "BIO";

        if (manualType == null) return false;

        float hp = Math.max(12.0F, (float) Math.ceil(target.getMaxHealth() * 0.25F));
        ListTag list = new ListTag();
        CompoundTag layer = new CompoundTag();
        layer.putString("Type", manualType);
        layer.putFloat("HP", hp);
        layer.putFloat("MaxHP", hp);
        list.add(layer);

        persistentData.put(MobArchetypes.NBT_SHIELD_LAYERS, list);
        persistentData.putInt(MobArchetypes.NBT_CURRENT_LAYER_INDEX, 0);
        persistentData.putBoolean(MobArchetypes.NBT_ALL_SHIELDS_BROKEN, false);
        return true;
    }
}