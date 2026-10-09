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
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class CasterResonanceHandler {

    private static final Pattern GLYPH_PATTERN = Pattern.compile("([a-z0-9_]+:)?glyph_[a-z0-9_]+");

    /**
     * Прямое применение перегрузки (+1 к частоте на 5 секунд)
     */
    private static void triggerSurge(ServerPlayer player, ItemStack stack, int durationTicks) {
        if (stack.isEmpty()) return;

        CompoundTag pData = player.getPersistentData();
        long currentTick = player.level().getGameTime();

        // Защита от дублирования в одном тике
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
     * Безопасное чтение маны через API Ars Nouveau (ManaHelper / Capability)
     */
    public static double getPlayerMana(ServerPlayer player) {
        if (player == null) return -1.0;

        // Попытка 1: ManaHelper.getCurrentMana(LivingEntity)
        try {
            Class<?> helper = Class.forName("com.hollingsworth.arsnouveau.api.mana.ManaHelper");
            for (Method m : helper.getMethods()) {
                if (m.getParameterCount() == 1 && (m.getName().equals("getCurrentMana") || m.getName().equals("getMana"))) {
                    Object res = m.invoke(null, player);
                    if (res instanceof Number num) {
                        return num.doubleValue();
                    }
                }
            }
        } catch (Throwable ignored) {}

        // Попытка 2: CapabilityRegistry.MANA_CAPABILITY
        String[] capClasses = {
                "com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry",
                "com.hollingsworth.arsnouveau.common.capability.CapabilityRegistry"
        };
        for (String cName : capClasses) {
            try {
                Class<?> reg = Class.forName(cName);
                for (Field f : reg.getFields()) {
                    if (f.getName().contains("MANA")) {
                        Object capObj = f.get(null);
                        if (capObj instanceof net.minecraftforge.common.capabilities.Capability<?> cap) {
                            var lazy = player.getCapability(cap);
                            if (lazy.isPresent()) {
                                Object manaObj = lazy.orElse(null);
                                if (manaObj != null) {
                                    for (Method m : manaObj.getClass().getMethods()) {
                                        if (m.getParameterCount() == 0 && (m.getName().equals("getCurrentMana") || m.getName().equals("getMana"))) {
                                            Object res = m.invoke(manaObj);
                                            if (res instanceof Number num) return num.doubleValue();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // Попытка 3: Прямой опрос CapabilityManager
        try {
            for (Field f : net.minecraftforge.common.capabilities.CapabilityManager.class.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(net.minecraftforge.common.capabilities.CapabilityManager.INSTANCE);
                if (val instanceof Map<?, ?> map) {
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        String keyStr = String.valueOf(entry.getKey()).toLowerCase();
                        if (keyStr.contains("mana")) {
                            if (entry.getValue() instanceof net.minecraftforge.common.capabilities.Capability<?> cap) {
                                var lazy = player.getCapability(cap);
                                if (lazy.isPresent()) {
                                    Object manaObj = lazy.orElse(null);
                                    if (manaObj != null) {
                                        for (Method m : manaObj.getClass().getMethods()) {
                                            if (m.getParameterCount() == 0 && (m.getName().equals("getCurrentMana") || m.getName().equals("getMana"))) {
                                                Object res = m.invoke(manaObj);
                                                if (res instanceof Number num) return num.doubleValue();
                                            }
                                        }
                                    }
                                }
                            }
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

    private static boolean isArsBow(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("bow") && !rl.getPath().contains("crossbow");
    }

    // =========================================================================
    // ПЕРЕХВАТ СОБЫТИЙ: СТРЕЛЬБА И БОЙ
    // =========================================================================

    // 1. Отпускание тетивы лука: фиксируем запас маны ДО выстрела
    @SubscribeEvent
    public static void onUseItemStop(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = event.getItem();
            if (isArsBow(stack) && !extractActiveGlyphs(stack).isEmpty()) {
                double mana = getPlayerMana(player);
                player.getPersistentData().putDouble("ars_shot_mana_before", mana);
                player.getPersistentData().putLong("ars_shot_tick", player.level().getGameTime());
            }
        }
    }

    // 2. Спуск заряженного арбалета: фиксируем ману ДО выстрела
    @SubscribeEvent
    public static void onItemRightClickCrossbow(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = event.getItemStack();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("crossbow") && CrossbowItem.isCharged(stack)) {
                if (!extractActiveGlyphs(stack).isEmpty()) {
                    double mana = getPlayerMana(player);
                    player.getPersistentData().putDouble("ars_shot_mana_before", mana);
                    player.getPersistentData().putLong("ars_shot_tick", player.level().getGameTime());
                }
            }
        }
    }

    // 3. Вылет стрелы в мир: сверяем ману ПОСЛЕ каста
    @SubscribeEvent
    public static void onProjectileSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof ServerPlayer player) {
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            ItemStack weapon = isArsWeapon(mainHand) ? mainHand : (isArsWeapon(offHand) ? offHand : ItemStack.EMPTY);

            if (weapon.isEmpty() || extractActiveGlyphs(weapon).isEmpty()) return;

            CompoundTag pData = player.getPersistentData();
            long currentTick = player.level().getGameTime();

            if (pData.contains("ars_shot_mana_before") && pData.getLong("ars_shot_tick") == currentTick) {
                double manaBefore = pData.getDouble("ars_shot_mana_before");
                double manaNow = getPlayerMana(player);

                pData.remove("ars_shot_mana_before");
                pData.remove("ars_shot_tick");

                // Мана уменьшилась хотя бы на 0.5, либо режим творческий, либо фоллбек
                boolean manaSpent = (manaBefore >= 0.0 && manaNow >= 0.0 && manaNow < (manaBefore - 0.5))
                                 || player.isCreative()
                                 || (manaBefore < 0.0);

                if (manaSpent) {
                    triggerSurge(player, weapon, 100);
                }
            }
        }
    }

    // 4. Удар мечом: фиксируем ману в момент замаха
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack held = player.getMainHandItem();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(held.getItem());
            if (rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("sword")) {
                if (!extractActiveGlyphs(held).isEmpty()) {
                    double mana = getPlayerMana(player);
                    player.getPersistentData().putDouble("ars_sword_mana_before", mana);
                    player.getPersistentData().putLong("ars_sword_tick", player.level().getGameTime());
                }
            }
        }
    }

    // 5. Завершение тика атаки мечом: сверяем списание маны
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        CompoundTag pData = player.getPersistentData();
        if (!pData.contains("ars_sword_mana_before")) return;

        long currentTick = player.level().getGameTime();
        if (pData.getLong("ars_sword_tick") == currentTick) {
            double manaBefore = pData.getDouble("ars_sword_mana_before");
            double manaNow = getPlayerMana(player);

            pData.remove("ars_sword_mana_before");
            pData.remove("ars_sword_tick");

            boolean manaSpent = (manaBefore >= 0.0 && manaNow >= 0.0 && manaNow < (manaBefore - 0.5))
                             || player.isCreative()
                             || (manaBefore < 0.0);

            if (manaSpent) {
                ItemStack held = player.getMainHandItem();
                triggerSurge(player, held, 100);
            }
        } else {
            pData.remove("ars_sword_mana_before");
            pData.remove("ars_sword_tick");
        }
    }

    private static boolean isArsWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null || !rl.getNamespace().startsWith("ars_")) return false;
        String p = rl.getPath();
        return p.contains("bow") || p.contains("crossbow");
    }
}