package com.example.arcanebridge.logic;

import com.example.arcanebridge.capability.ResonanceProvider;
import com.example.arcanebridge.network.ClientboundTendrilStrikePacket;
import com.example.arcanebridge.network.NetworkHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
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
        float currentLoad = currentTimer > 0 ? (pData.contains(loadKey, 5) ? pData.getFloat(loadKey) : (float) pData.getInt(loadKey)) : 0.0f;
        float newLoad = currentLoad + 1.0f;

        pData.putInt(timerKey, durationTicks);
        pData.putFloat(loadKey, newLoad);

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

    // 4. Удар по сущности мечом Ars: фиксируем ману в момент замаха
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

    // 5. Завершение тика игрока: накопление заряда щупалец и учет маны меча
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        CompoundTag pData = player.getPersistentData();

        // Затухание активного режима «Пробуждения Щупалец» (всегда 5 секунд / 100 тиков)
        int awakeTicks = pData.getInt("tendril_awakening_ticks");
        if (awakeTicks > 0) {
            pData.putInt("tendril_awakening_ticks", awakeTicks - 1);
            if (awakeTicks - 1 == 0) {
                pData.putInt("tendril_awakening_power", 0);
                player.serverLevel().playSound(null, player.getX(), player.getY() + 1.2, player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 1.8f);
            }
        }

        // Таймер удержания заряда (10 секунд = 200 тиков при отпускании Shift)
        int stacks = pData.getInt("tendril_stacks");
        if (stacks > 0 && awakeTicks <= 0) {
            if (player.isCrouching()) {
                pData.putInt("tendril_hold_ticks", 200);
            } else {
                int holdTicks = pData.getInt("tendril_hold_ticks") - 1;
                pData.putInt("tendril_hold_ticks", holdTicks);
                if (holdTicks <= 0) {
                    pData.putInt("tendril_stacks", 0);
                    pData.putInt("tendril_charge_ticks", 0);
                    pData.putLong("tendril_cooldown_until", player.level().getGameTime() + 100); // 5 сек кулдауна после сброса
                    pData.putInt("tendril_max_cooldown", 100);
                    player.serverLevel().playSound(null, player.getX(), player.getY() + 1.2, player.getZ(),
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 0.6f);
                    player.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT,
                            player.getX(), player.getY() + 1.2, player.getZ(), 10, 0.2, 0.2, 0.2, 0.02);
                }
            }
        }

        // Накопление стаков щупалец при удержании Shift (1-4 стака)
        handleTendrilCharge(player);

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

    /**
     * Зарядка щупалец за спиной при удержании Shift (1-4 стака).
     * Заряд НЕ сбрасывается при отпускании Shift — остаётся готовым к атаке!
     */
    private static void handleTendrilCharge(ServerPlayer player) {
        CompoundTag pData = player.getPersistentData();
        long currentTick = player.level().getGameTime();

        long cdUntil = pData.getLong("tendril_cooldown_until");
        if (currentTick < cdUntil) return;

        float stability = player.getCapability(ResonanceProvider.RESONANCE)
                .map(r -> r.getStability()).orElse(100.0f);
        boolean isMagic = pData.getBoolean("ArcaneArcaneOverload") || pData.getBoolean("ArcaneEleOverload")
                || pData.getFloat("arcane_caster_load") > 0 || pData.getFloat("ele_caster_load") > 0;

        if (stability <= 50.0f && isMagic && player.isCrouching()) {
            int chargeTicks = pData.getInt("tendril_charge_ticks") + 1;
            pData.putInt("tendril_charge_ticks", chargeTicks);

            int oldStacks = pData.getInt("tendril_stacks");
            int newStacks = 0;
            if (chargeTicks >= 90) newStacks = 4;
            else if (chargeTicks >= 60) newStacks = 3;
            else if (chargeTicks >= 35) newStacks = 2;
            else if (chargeTicks >= 15) newStacks = 1;

            if (newStacks > oldStacks) {
                pData.putInt("tendril_stacks", newStacks);
                float pitch = 0.85f + (newStacks * 0.28f);
                player.serverLevel().playSound(null, player.getX(), player.getY() + 1.2, player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.85f, pitch);
                player.serverLevel().playSound(null, player.getX(), player.getY() + 1.2, player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.45f, pitch);

                if (newStacks == 4) {
                    player.serverLevel().playSound(null, player.getX(), player.getY() + 1.2, player.getZ(),
                            SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.9f, 1.2f);
                }

                player.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT,
                        player.getX(), player.getY() + 1.2, player.getZ(),
                        newStacks * 5, 0.25, 0.25, 0.25, 0.06);
            }
        } else if (stability > 50.0f && !isMagic) {
            pData.putInt("tendril_stacks", 0);
            pData.putInt("tendril_charge_ticks", 0);
        }
    }

    /**
     * Срабатывание при нанесении ЛЮБОГО урона игроком (рукой, мечом, луком, заклинанием).
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            LivingEntity victim = event.getEntity();
            if (victim == player) return;

            // Активация механического форсажа при ударе
            ResonanceStabilizationRituals.triggerKineticOverclock(player);

            CompoundTag pData = player.getPersistentData();
            if (pData.getBoolean("tendril_strike_in_progress")) return;

            long currentTick = player.level().getGameTime();
            int stacks = pData.getInt("tendril_stacks");
            int awakeTicks = pData.getInt("tendril_awakening_ticks");
            int awakePower = pData.getInt("tendril_awakening_power");

            // 1. Если был накоплен заряд — пробуждаем щупальца на время (режим помощи в бою)!
            if (stacks > 0 && awakeTicks <= 0) {
                pData.putInt("tendril_stacks", 0);
                pData.putInt("tendril_charge_ticks", 0);
                pData.putInt("tendril_hold_ticks", 0);

                int duration = 100; // ВСЕГДА ровно 5 секунд (100 тиков)!
                pData.putInt("tendril_awakening_ticks", duration);
                pData.putInt("tendril_awakening_power", stacks);
                pData.putLong("last_tendril_assist_tick", currentTick);

                // Кулдаун после спуска: 5 сек действия + stacks * 5 сек отката (5с для тира 1, 10с для тира 2, 15с для тира 3, 20с для тира 4)
                int cdTicks = stacks * 100;
                pData.putLong("tendril_cooldown_until", currentTick + duration + cdTicks);
                pData.putInt("tendril_max_cooldown", cdTicks);

                triggerTendrilTargetedStrike(player, victim, stacks, true);
            } else if (awakeTicks > 0) {
                // 2. Во время активного режима щупальца ассистируют при каждом ударе игрока (кд 12 тиков / 0.6 сек)
                long lastAssist = pData.getLong("last_tendril_assist_tick");
                if (currentTick - lastAssist >= 12) {
                    pData.putLong("last_tendril_assist_tick", currentTick);
                    triggerTendrilTargetedStrike(player, victim, Math.max(1, awakePower), false);
                }
            }
        }
    }

    /**
     * Ускорение натяжения тетивы луков и зарядки арбалетов при активном «Кинетическом форсаже» (до х3).
     */
    @SubscribeEvent
    public static void onUseItemTick(LivingEntityUseItemEvent.Tick event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player
                && player.hasEffect(com.example.arcanebridge.registry.ModEffects.KINETIC_OVERCLOCK.get())) {
            int amp = player.getEffect(com.example.arcanebridge.registry.ModEffects.KINETIC_OVERCLOCK.get()).getAmplifier();
            int bonus = (amp >= 2) ? 2 : 1; // +1 даёт х2 скорость, +2 даёт ровно х3 скорость!
            event.setDuration(Math.max(0, event.getDuration() - bonus));
        }
    }

    /**
     * Направленный выпад щупалец из спины по цели, получившей урон.
     */
    public static void triggerTendrilTargetedStrike(ServerPlayer player, LivingEntity victim, int stacks, boolean initialAwakening) {
        ServerLevel level = player.serverLevel();
        CompoundTag pData = player.getPersistentData();
        long currentTick = level.getGameTime();
        boolean ele = pData.getBoolean("ArcaneEleOverload") || pData.getFloat("ele_caster_load") > pData.getFloat("arcane_caster_load");

        // 1. Урон по цели: начальный пробуждающий удар мощнее, последующие ассисты бьют темпово
        float bonusDamage;
        if (initialAwakening) {
            bonusDamage = switch (stacks) {
                case 1 -> 5.0f;
                case 2 -> 9.0f;
                case 3 -> 14.0f;
                default -> 20.0f;
            };
        } else {
            bonusDamage = switch (stacks) {
                case 1 -> 3.5f;
                case 2 -> 6.0f;
                case 3 -> 9.0f;
                default -> 13.0f;
            };
        }

        boolean isSatiated = player.hasEffect(com.example.arcanebridge.registry.ModEffects.CRYSTALLINE_SATIATION.get());
        int satiationAmp = isSatiated ? player.getEffect(com.example.arcanebridge.registry.ModEffects.CRYSTALLINE_SATIATION.get()).getAmplifier() : -1;
        if (satiationAmp >= 1) {
            bonusDamage *= 1.30f; // +30% урона щупалец на Тир 2 и Тир 3!
        }

        // 2. Начисление перегрузки происходит только при первичном входе в режим
        if (initialAwakening) {
            float baseLoad = switch (stacks) {
                case 1 -> 0.5f;
                case 2 -> 1.0f;
                case 3 -> 1.5f;
                default -> 2.0f;
            };

            boolean hasMastery = pData.getBoolean("MatrixTendrilMastery");

            float loadAdded = hasMastery ? 0.0f : (isSatiated ? (baseLoad * 0.25f) : baseLoad);

            int shockDuration = switch (stacks) {
                case 1 -> 60;
                case 2 -> 90;
                case 3 -> 120;
                default -> 160;
            };
            String loadKey = ele ? "ele_caster_load" : "arcane_caster_load";
            String timerKey = ele ? "ele_shock_timer" : "arcane_shock_timer";

            if (loadAdded > 0.0f) {
                float curLoad = pData.contains(loadKey, 5) ? pData.getFloat(loadKey) : (float) pData.getInt(loadKey);
                pData.putFloat(loadKey, curLoad + loadAdded);
                pData.putInt(timerKey, shockDuration);
            }

            // Отдача игроку на 4 щупальцах (полностью блокируется синергией матриц или насыщением аметистом!)
            if (stacks >= 4 && !isSatiated && !hasMastery) {
                player.hurt(player.damageSources().magic(), 1.5f);
                player.hurtMarked = true;
            }
        }

        // 3. Нанесение урона жертве
        pData.putBoolean("tendril_strike_in_progress", true);
        try {
            victim.hurt(player.damageSources().magic(), bonusDamage);
        } finally {
            pData.putBoolean("tendril_strike_in_progress", false);
        }

        // Перк Тир 3 «Разрыв Пустоты» (Void Tear): микро-воронка при первичном ударе или каждом 3-м ассисте
        if (satiationAmp >= 2) {
            int assistCount = pData.getInt("tendril_assist_count") + 1;
            pData.putInt("tendril_assist_count", assistCount);
            if (initialAwakening || assistCount % 3 == 0) {
                triggerVoidTear(player, victim);
            }
        }

        // Отбрасывание цели
        Vec3 knockDir = victim.position().subtract(player.position()).normalize();
        double knockPower = 0.40D + (stacks * 0.30D);
        victim.setDeltaMovement(knockDir.x * knockPower, 0.25D + (stacks * 0.08D), knockDir.z * knockPower);
        victim.hurtMarked = true;

        // 4. Синхронизация светящихся щупалец Ars Nouveau от спины игрока к цели
        double yawRad = Math.toRadians(player.yBodyRot);
        double backX = Math.sin(yawRad);
        double backZ = -Math.cos(yawRad);
        double px = player.getX() + backX * 0.20D;
        double py = player.getY() + 1.25D;
        double pz = player.getZ() + backZ * 0.20D;

        double vx = victim.getX();
        double vy = victim.getY() + (victim.getBbHeight() * 0.5D);
        double vz = victim.getZ();

        NetworkHandler.sendToTrackingAndSelf(
                new ClientboundTendrilStrikePacket(px, py, pz, vx, vy, vz, stacks, ele),
                player
        );

        // 5. Звуки удара в зависимости от числа щупалец
        switch (stacks) {
            case 1 -> {
                level.playSound(null, vx, vy, vz, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.85f, 0.85f);
                level.playSound(null, vx, vy, vz, SoundEvents.AMETHYST_CLUSTER_HIT, SoundSource.PLAYERS, 0.95f, 1.4f);
            }
            case 2 -> {
                level.playSound(null, vx, vy, vz, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.95f, 0.8f);
                level.playSound(null, vx, vy, vz, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0f, 1.3f);
            }
            case 3 -> {
                level.playSound(null, vx, vy, vz, SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 0.95f, 1.1f);
                level.playSound(null, vx, vy, vz, SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.85f, 1.3f);
                level.playSound(null, vx, vy, vz, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            default -> {
                level.playSound(null, vx, vy, vz, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 1.35f);
                level.playSound(null, vx, vy, vz, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.85f, 1.5f);
                level.playSound(null, vx, vy, vz, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0f, 0.85f);
                level.sendParticles(ParticleTypes.SONIC_BOOM, vx, vy, vz, 1, 0, 0, 0, 0);
            }
        }
        level.sendParticles(ParticleTypes.FLASH, vx, vy, vz, 1, 0, 0, 0, 0);
    }

    /**
     * Перк «Разрыв Пустоты» (Void Tear) — пространственная микро-воронка, втягивающая мобов в эпицентр
     */
    public static void triggerVoidTear(ServerPlayer player, LivingEntity victim) {
        ServerLevel level = player.serverLevel();
        double vx = victim.getX();
        double vy = victim.getY() + (victim.getBbHeight() * 0.5D);
        double vz = victim.getZ();

        level.playSound(null, vx, vy, vz, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.75f, 1.6f);
        level.playSound(null, vx, vy, vz, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.9f, 1.8f);

        level.sendParticles(ParticleTypes.SONIC_BOOM, vx, vy, vz, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.PORTAL, vx, vy, vz, 30, 0.5, 0.5, 0.5, 0.2);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, vx, vy, vz, 20, 0.4, 0.4, 0.4, 0.1);

        net.minecraft.world.phys.AABB pullBox = victim.getBoundingBox().inflate(5.0);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, pullBox, e -> e != player && e != victim && e.isAlive());

        for (LivingEntity e : nearby) {
            Vec3 pullDir = victim.position().subtract(e.position()).normalize();
            e.setDeltaMovement(pullDir.x * 0.55, 0.25, pullDir.z * 0.55);
            e.hurt(player.damageSources().magic(), 6.0f);
            e.hurtMarked = true;
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