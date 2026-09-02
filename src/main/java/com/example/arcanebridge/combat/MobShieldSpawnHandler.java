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
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.util.Random;

@Mod.EventBusSubscriber(modid = MobArchetypes.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MobShieldSpawnHandler {

    private static final String NBT_INITIALIZED = "arcane_shield_initialized";
    private static final Random RNG = new Random();

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // Игнорируем игроков, манекены и мирных животных
        if (target instanceof Player || target instanceof ArmorStand) return;
        if (target instanceof Animal || target instanceof WaterAnimal || target instanceof AmbientCreature) return;

        CompoundTag persistentData = target.getPersistentData();
        if (persistentData.getBoolean(NBT_INITIALIZED)) return;
        persistentData.putBoolean(NBT_INITIALIZED, true);

        // Если у моба уже прописаны кастомные теги из командных блоков — используем их
        if (tryApplyCommandTags(target, persistentData)) return;

        // Проверка базового запаса здоровья сущности
        float maxHp = target.getMaxHealth();
        if (maxHp < 25.0F) {
            // Ранг 0 (Рядовой / Fodder) — 0% шанс на щит
            return;
        }

        Level level = event.getLevel();
        String stage = getMajruszStage(level);

        int layerCount = 0;
        float minPercent = 0.20F;
        float maxPercent = 0.25F;

        // ----------------------------------------------------
        // 1. Определение ранга и расчет шансов по фазам мира
        // ----------------------------------------------------
        if (maxHp < 45.0F) {
            // Ранг 1: Ветеран (25 - 44 HP)
            float chance = switch (stage) {
                case "MASTER" -> 0.60F;
                case "EXPERT" -> 0.35F;
                default -> 0.10F;
            };
            if (RNG.nextFloat() < chance) {
                layerCount = 1;
                minPercent = 0.20F;
                maxPercent = 0.25F;
            }
        } else if (maxHp < 80.0F) {
            // Ранг 2: Элита (45 - 79 HP)
            float chance = switch (stage) {
                case "MASTER" -> 1.00F;
                case "EXPERT" -> 0.80F;
                default -> 0.40F;
            };
            if (RNG.nextFloat() < chance) {
                minPercent = 0.25F;
                maxPercent = 0.35F;
                if (stage.equals("NORMAL")) {
                    layerCount = 1;
                } else if (stage.equals("EXPERT")) {
                    layerCount = (RNG.nextFloat() < 0.25F) ? 2 : 1;
                } else { // MASTER
                    layerCount = (RNG.nextFloat() < 0.50F) ? 2 : 1;
                }
            }
        } else if (maxHp <= 160.0F) {
            // Ранг 3: Мини-босс / Чемпион (80 - 160 HP)
            minPercent = 0.35F;
            maxPercent = 0.45F;
            if (stage.equals("MASTER") && RNG.nextFloat() < 0.30F) {
                layerCount = 3;
            } else {
                layerCount = 2;
            }
        } else {
            // Ранг 4: Босс / Титан (> 160 HP)
            layerCount = 3;
            minPercent = 0.40F;
            maxPercent = 0.50F;
        }

        if (layerCount <= 0) return;

        // ----------------------------------------------------
        // 2. Определение архетипов и создание NBT слоев
        // ----------------------------------------------------
        ListTag layersList = new ListTag();

        for (int i = 0; i < layerCount; i++) {
            String archetype = pickArchetypeForEntity(target, i, layerCount);
            float layerRatio = minPercent + RNG.nextFloat() * (maxPercent - minPercent);
            float layerHp = (float) Math.ceil(maxHp * layerRatio);

            CompoundTag layerTag = new CompoundTag();
            layerTag.putString("Type", archetype);
            layerTag.putFloat("HP", layerHp);
            layerTag.putFloat("MaxHP", layerHp);
            layersList.add(layerTag);
        }

        persistentData.put(MobArchetypes.NBT_SHIELD_LAYERS, layersList);
        persistentData.putInt(MobArchetypes.NBT_CURRENT_LAYER_INDEX, 0);
        persistentData.putBoolean(MobArchetypes.NBT_ALL_SHIELDS_BROKEN, false);
    }

    /**
     * Интеллектуальный выбор архетипа щита на основе снаряжения и физиологии моба
     */
    private static String pickArchetypeForEntity(LivingEntity entity, int layerIndex, int totalLayers) {
        // При многослойных щитах формируем тактическую комбинацию: внешний слой часто Эфирный/Био, внутренний — Броня
        if (totalLayers >= 2 && layerIndex == totalLayers - 1) {
            if (hasHeavyArmor(entity) || RNG.nextFloat() < 0.65F) {
                return "ARMORED";
            }
        }

        int armoredWeight = 40;
        int etherealWeight = 30;
        int bioWeight = 30;

        // 1. Контекст металлической/тяжелой экипировки
        if (hasHeavyArmor(entity)) {
            armoredWeight = 75;
            etherealWeight = 15;
            bioWeight = 10;
        }
        // 2. Контекст магии, летающих созданий и обитателей Энда
        else if (isMagicOrAirEntity(entity)) {
            armoredWeight = 10;
            etherealWeight = 80;
            bioWeight = 10;
        }
        // 3. Контекст членистоногих, болотной нежити и мутантов
        else if (isBioEntity(entity)) {
            armoredWeight = 10;
            etherealWeight = 10;
            bioWeight = 80;
        }

        int totalWeight = armoredWeight + etherealWeight + bioWeight;
        int roll = RNG.nextInt(totalWeight);

        if (roll < armoredWeight) return "ARMORED";
        if (roll < armoredWeight + etherealWeight) return "ETHEREAL";
        return "BIO";
    }

    private static boolean hasHeavyArmor(LivingEntity entity) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.getItem() instanceof ArmorItem armor) {
                var mat = armor.getMaterial();
                if (mat == ArmorMaterials.IRON || mat == ArmorMaterials.DIAMOND ||
                    mat == ArmorMaterials.NETHERITE || mat == ArmorMaterials.CHAIN) {
                    return true;
                }
                String name = mat.getName().toLowerCase();
                if (name.contains("brass") || name.contains("steel") || name.contains("enderium")) {
                    return true;
                }
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

    /**
     * Считывание текущей глобальной фазы сложности Majrusz's Progressive Difficulty
     */
    private static String getMajruszStage(Level level) {
        try {
            Class<?> helperClass = Class.forName("com.majruszsdifficulty.gamestage.GameStageHelper");
            for (Method m : helperClass.getMethods()) {
                if (m.getParameterCount() == 0 && (m.getName().equals("getGameStage") || m.getName().equals("getCurrentStage"))) {
                    Object stageObj = m.invoke(null);
                    if (stageObj != null) {
                        return stageObj.toString().toUpperCase();
                    }
                }
            }
        } catch (Throwable ignored) {}

        // Fallback: привязка к измерениям, если фаза не считалась
        if (level.dimension() == Level.END) return "MASTER";
        if (level.dimension() == Level.NETHER) return "EXPERT";
        return "NORMAL";
    }

    /**
     * Поддержка тестовых тегов из команд /summon
     */
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