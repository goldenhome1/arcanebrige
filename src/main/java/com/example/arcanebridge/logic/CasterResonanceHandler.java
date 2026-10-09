package com.example.arcanebridge.logic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class CasterResonanceHandler {

    private static final Pattern GLYPH_PATTERN = Pattern.compile("([a-z0-9_]+:)?glyph_[a-z0-9_]+");

    // Кэш рефлексии для безопасного считывания маны Ars Nouveau
    private static Method getManaMethod = null;
    private static Field manaCapField = null;
    private static boolean reflectionChecked = false;

    /**
     * Постановка действия на проверку: проверяем расход маны в течение следующих 2-3 тиков
     */
    private static void queueSurgeCheck(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;

        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null || !rl.getNamespace().startsWith("ars_")) return;

        List<String> glyphs = extractActiveGlyphs(stack);
        if (glyphs.isEmpty()) return; // Оружие без магии игнорируется

        double currentMana = getPlayerMana(player);

        // Если не удалось прочитать ману через API, делаем прямой триггер (fallback)
        if (currentMana < 0.0) {
            triggerSurge(player, stack, 100);
            return;
        }

        CompoundTag pData = player.getPersistentData();
        pData.putInt("caster_check_ticks", 3);
        pData.putDouble("caster_mana_before", currentMana);
        pData.put("caster_pending_weapon", stack.save(new CompoundTag()));
    }

    /**
     * Проверка списания маны на серверном тике игрока
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        CompoundTag pData = player.getPersistentData();
        if (!pData.contains("caster_check_ticks")) return;

        int ticksLeft = pData.getInt("caster_check_ticks");
        double manaBefore = pData.getDouble("caster_mana_before");
        double manaNow = getPlayerMana(player);

        // Условие: мана реально уменьшилась (хотя бы на 0.1) ИЛИ игрок в креативе
        boolean manaSpent = (manaNow >= 0.0 && manaBefore >= 0.0 && manaNow < (manaBefore - 0.1));
        boolean isCreative = player.isCreative();

        if (manaSpent || isCreative) {
            // Мана потрачена -> применяем перегрузку
            ItemStack weapon = ItemStack.of(pData.getCompound("caster_pending_weapon"));
            triggerSurge(player, weapon, 100);

            pData.remove("caster_check_ticks");
            pData.remove("caster_mana_before");
            pData.remove("caster_pending_weapon");
            return;
        }

        ticksLeft--;
        if (ticksLeft <= 0) {
            // Мана не была списана (выстрел был без магии) -> отменяем без перегруза
            pData.remove("caster_check_ticks");
            pData.remove("caster_mana_before");
            pData.remove("caster_pending_weapon");
        } else {
            pData.putInt("caster_check_ticks", ticksLeft);
        }
    }

    /**
     * Начисление +1 к частоте
     */
    private static void triggerSurge(ServerPlayer player, ItemStack stack, int durationTicks) {
        if (stack.isEmpty()) return;

        CompoundTag pData = player.getPersistentData();
        long currentTick = player.level().getGameTime();

        if (pData.getLong("last_caster_surge_tick") == currentTick) return;
        pData.putLong("last_caster_surge_tick", currentTick);

        List<String> glyphs = extractActiveGlyphs(stack);
        boolean isElemental = isElementalSpell(glyphs);

        String timerKey = isElemental ? "ele_shock_timer" : "arcane_shock_timer";
        String loadKey = isElemental ? "ele_caster_load" : "arcane_caster_load";

        int currentTimer = pData.getInt(timerKey);
        int currentLoad = currentTimer > 0 ? pData.getInt(loadKey) : 0;
        int newLoad = currentLoad + 1;

        pData.putInt(timerKey, durationTicks);
        pData.putInt(loadKey, newLoad);

        ServerLevel level = player.serverLevel();
        double px = player.getX();
        double py = player.getY() + 1.1D;
        double pz = player.getZ();

        if (isElemental) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 10, 0.25D, 0.25D, 0.25D, 0.04D);
            level.playSound(null, px, py, pz, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7F, 1.3F);
        } else {
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, px, py, pz, 14, 0.3D, 0.3D, 0.3D, 0.08D);
            level.playSound(null, px, py, pz, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8F, 1.4F);
        }
    }

    /**
     * Безопасное чтение текущей маны Ars Nouveau у игрока
     */
    public static double getPlayerMana(ServerPlayer player) {
        if (player == null) return -1.0;

        if (!reflectionChecked) {
            reflectionChecked = true;
            String[] possibleRegistries = {
                    "com.hollingsworth.arsnouveau.common.capability.CapabilityRegistry",
                    "com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry",
                    "com.hollingsworth.arsnouveau.api.mana.ManaHelper"
            };

            for (String reg : possibleRegistries) {
                try {
                    Class<?> clazz = Class.forName(reg);
                    for (Method m : clazz.getMethods()) {
                        if (m.getName().equals("getMana") && m.getParameterCount() == 1) {
                            getManaMethod = m;
                            break;
                        }
                    }
                    if (getManaMethod != null) break;
                } catch (Throwable ignored) {}
            }

            for (String reg : possibleRegistries) {
                try {
                    Class<?> clazz = Class.forName(reg);
                    for (Field f : clazz.getFields()) {
                        if (f.getName().contains("MANA")) {
                            manaCapField = f;
                            break;
                        }
                    }
                    if (manaCapField != null) break;
                } catch (Throwable ignored) {}
            }
        }

        try {
            Object manaObj = null;

            if (getManaMethod != null) {
                Object res = getManaMethod.invoke(null, player);
                if (res instanceof net.minecraftforge.common.util.LazyOptional<?> lazy) {
                    manaObj = lazy.orElse(null);
                } else {
                    manaObj = res;
                }
            }

            if (manaObj == null && manaCapField != null) {
                Object cap = manaCapField.get(null);
                if (cap instanceof net.minecraftforge.common.capabilities.Capability<?> forgeCap) {
                    var lazy = player.getCapability(forgeCap);
                    manaObj = lazy.orElse(null);
                }
            }

            if (manaObj != null) {
                for (Method m : manaObj.getClass().getMethods()) {
                    if ((m.getName().equals("getCurrentMana") || m.getName().equals("getMana")) && m.getParameterCount() == 0) {
                        Object val = m.invoke(manaObj);
                        if (val instanceof Number num) {
                            return num.doubleValue();
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        return -1.0;
    }

    private static List<String> extractActiveGlyphs(ItemStack stack) {
        List<String> glyphs = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null) return glyphs;

        String nbt = tag.toString().toLowerCase();
        Matcher matcher = GLYPH_PATTERN.matcher(nbt);
        while (matcher.find()) {
            String g = matcher.group();
            if (!glyphs.contains(g)) {
                glyphs.add(g);
            }
        }
        return glyphs;
    }

    private static boolean isElementalSpell(List<String> glyphs) {
        for (String g : glyphs) {
            String lower = g.toLowerCase();
            if (lower.contains("ars_elemental")) return true;

            if (lower.contains("glyph_flare") || lower.contains("glyph_ignite") || lower.contains("glyph_firework")
             || lower.contains("glyph_freeze") || lower.contains("glyph_cold_snap") || lower.contains("glyph_conjure_water")
             || lower.contains("glyph_crush")
             || lower.contains("glyph_wind") || lower.contains("glyph_gust") || lower.contains("glyph_launch")
             || lower.contains("glyph_lightning")) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // ПЕРЕХВАТЧИКИ СОБЫТИЙ (ЛУК, АРБАЛЕТ, МЕЧ)
    // =========================================================================

    // 1. Лук чародея (отпускание тетивы)
    @SubscribeEvent
    public static void onUseItemStop(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = event.getItem();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("bow") && !rl.getPath().contains("crossbow")) {
                queueSurgeCheck(player, stack);
            }
        }
    }

    // 2. Ванильный хук лука
    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack bow = event.getBow();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(bow.getItem());
            if (rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("bow")) {
                queueSurgeCheck(player, bow);
            }
        }
    }

    // 3. Вылет стрелы в мир
    @SubscribeEvent
    public static void onProjectileSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof ServerPlayer player) {
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            ItemStack weapon = isArsBow(mainHand) ? mainHand : (isArsBow(offHand) ? offHand : ItemStack.EMPTY);

            if (!weapon.isEmpty()) {
                queueSurgeCheck(player, weapon);
            }
        }
    }

    // 4. Удар Мечом чародея
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack held = player.getMainHandItem();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(held.getItem());
            if (rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("sword")) {
                queueSurgeCheck(player, held);
            }
        }
    }

    // 5. Выстрел из Арбалета чародея
    @SubscribeEvent
    public static void onItemRightClickCrossbow(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = event.getItemStack();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("crossbow") && CrossbowItem.isCharged(stack)) {
                queueSurgeCheck(player, stack);
            }
        }
    }

    private static boolean isArsBow(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("bow") && !rl.getPath().contains("crossbow");
    }
}