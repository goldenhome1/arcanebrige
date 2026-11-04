package com.example.arcanebridge.logic;

import com.example.arcanebridge.client.ResonanceVfxHandler;
import com.example.arcanebridge.network.ClientboundTendrilStrikePacket;
import com.example.arcanebridge.network.NetworkHandler;
import com.example.arcanebridge.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class ResonanceStabilizationRituals {

    // =========================================================================
    // 💎 1. РИТУАЛ МЕДИТАЦИИ В АМЕТИСТОВОЙ ЖЕОДЕ («КРИСТАЛЛИЧЕСКОЕ НАСЫЩЕНИЕ»)
    // =========================================================================
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        CompoundTag pData = player.getPersistentData();
        float stability = pData.getFloat("ArcaneStability");

        // 1. Медитация в аметистовой жеоде (присед рядом с аметистовыми кластерами)
        if (stability <= 50.0f && player.isCrouching()) {
            handleGeodeMeditation(player);
        } else {
            pData.putInt("geode_meditate_ticks", 0);
        }

        // 2. Механический оверклокинг при удержании Shift и таймер удержания заряда (10 секунд)
        boolean isMech = pData.getBoolean("ArcaneMechOverload") || pData.getFloat("mech_caster_load") > 0;
        int mechChargeTicks = pData.getInt("mech_charge_ticks");
        boolean hasOverclock = player.hasEffect(ModEffects.KINETIC_OVERCLOCK.get());

        if (stability <= 50.0f && isMech && player.isCrouching()) {
            handleMechCrouchCharging(player);
        } else if (mechChargeTicks >= 25 && !hasOverclock) {
            // Игрок отпустил шифт — тикают 10 секунд (200 тиков) удержания
            int holdTicks = pData.getInt("mech_hold_ticks") - 1;
            pData.putInt("mech_hold_ticks", holdTicks);
            if (holdTicks <= 0) {
                // 10 секунд истекли — механический заряд сбрасывается!
                pData.putInt("mech_charge_ticks", 0);
                pData.putLong("mech_overclock_cooldown", player.level().getGameTime() + 100); // 5 сек кд после сброса
                pData.putInt("mech_max_cooldown", 100);
                player.serverLevel().playSound(null, player.getX(), player.getY() + 1.0, player.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7f, 1.3f);
                player.serverLevel().sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.2, 0.2, 0.2, 0.03);
            }
        }
    }

    private static void handleGeodeMeditation(ServerPlayer player) {
        CompoundTag pData = player.getPersistentData();
        int medTicks = pData.getInt("geode_meditate_ticks") + 1;
        pData.putInt("geode_meditate_ticks", medTicks);

        // Каждые 24 тика (~1.2 сек) щупальца сканируют и ломают 1 созревший кластер аметиста
        if (medTicks % 24 == 0) {
            ServerLevel level = player.serverLevel();
            BlockPos playerPos = player.blockPosition();

            BlockPos foundCluster = null;
            double nearestDistSq = Double.MAX_VALUE;

            for (BlockPos target : BlockPos.betweenClosed(playerPos.offset(-4, -2, -4), playerPos.offset(4, 3, 4))) {
                BlockState state = level.getBlockState(target);
                if (state.is(Blocks.AMETHYST_CLUSTER)) {
                    double distSq = target.distSqr(playerPos);
                    if (distSq < nearestDistSq) {
                        nearestDistSq = distSq;
                        foundCluster = target.immutable();
                    }
                }
            }

            if (foundCluster != null) {
                // Щупальце протягивается и разбивает кластер
                double cx = foundCluster.getX() + 0.5;
                double cy = foundCluster.getY() + 0.5;
                double cz = foundCluster.getZ() + 0.5;

                NetworkHandler.sendToTrackingAndSelf(
                        new ClientboundTendrilStrikePacket(player.getX(), player.getY() + 1.2, player.getZ(), cx, cy, cz, 1, false),
                        player
                );

                level.destroyBlock(foundCluster, false); // Ломаем кластер без ванильного выпадения (поглощен!)
                level.playSound(null, cx, cy, cz, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 0.9f, 1.3f);
                level.playSound(null, player.getX(), player.getY() + 1.2, player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 1.6f);

                int eaten = pData.getInt("amethyst_clusters_eaten") + 1;
                pData.putInt("amethyst_clusters_eaten", eaten);

                ItemStack echoShard = findItem(player, "echo_shard");
                boolean hasEchoShard = !echoShard.isEmpty();

                if (eaten >= 35 && hasEchoShard) {
                    // Тир 3: 45 минут (35 кластеров + Осколок эха) -> перк «Разрыв Пустоты»
                    echoShard.shrink(1);
                    player.addEffect(new MobEffectInstance(ModEffects.CRYSTALLINE_SATIATION.get(), 45 * 60 * 20, 2, false, true, true));
                    level.playSound(null, player.getX(), player.getY() + 1.2, player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.85f, 1.4f);
                    level.playSound(null, player.getX(), player.getY() + 1.2, player.getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.0f, 1.2f);
                    level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1.2, player.getZ(), 1, 0, 0, 0, 0);
                    pData.putInt("amethyst_clusters_eaten", 0);
                } else if (eaten >= 20) {
                    if (!hasEchoShard) {
                        // Тир 2: 25 минут (20 кластеров) -> +30% урона щупалец
                        player.addEffect(new MobEffectInstance(ModEffects.CRYSTALLINE_SATIATION.get(), 25 * 60 * 20, 1, false, true, true));
                        level.playSound(null, player.getX(), player.getY() + 1.2, player.getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.9f, 1.4f);
                        pData.putInt("amethyst_clusters_eaten", 0);
                    } else {
                        // Есть Осколок эха — даем Тир 2 и продолжаем впитывать до 35!
                        player.addEffect(new MobEffectInstance(ModEffects.CRYSTALLINE_SATIATION.get(), 25 * 60 * 20, 1, false, true, true));
                        level.playSound(null, player.getX(), player.getY() + 1.2, player.getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.7f, 1.6f);
                    }
                } else if (eaten == 10) {
                    // Тир 1: 15 минут (10 кластеров) -> базовая стабилизация
                    player.addEffect(new MobEffectInstance(ModEffects.CRYSTALLINE_SATIATION.get(), 15 * 60 * 20, 0, false, true, true));
                    level.playSound(null, player.getX(), player.getY() + 1.2, player.getZ(), SoundEvents.AMETHYST_CLUSTER_HIT, SoundSource.PLAYERS, 1.0f, 1.5f);
                }
            }
        }
    }

    // =========================================================================
    // ⚙️ 3. РИТУАЛ КАЛИБРОВКИ МЕХАНИЗМОВ («КАЛИБРОВАННЫЕ СЕРВОПРИВОДЫ»)
    // =========================================================================
    @SubscribeEvent
    public static void onWrenchCalibration(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) return;

        ItemStack held = event.getItemStack();
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(held.getItem());
        if (rl == null || !rl.getPath().contains("wrench")) return;

        if (!player.isCrouching()) return;

        CompoundTag pData = player.getPersistentData();
        float stability = pData.getFloat("ArcaneStability");
        boolean isMech = pData.getBoolean("ArcaneMechOverload") || pData.getFloat("mech_caster_load") > 0;
        if (stability > 70.0f && !isMech) return;

        int alloyCount = countItem(player, "andesite_alloy");
        int copperCount = countItem(player, "copper_ingot");
        int precCount = countItem(player, "precision_mechanism");

        int tier;
        int durationTicks;

        if (alloyCount >= 24 && precCount >= 1) {
            // Тир 3: 45 минут (24 сплава + Точный механизм) -> перк «Сброс Пара»
            tier = 2;
            durationTicks = 45 * 60 * 20;
            deductItem(player, "andesite_alloy", 24);
            deductItem(player, "precision_mechanism", 1);
        } else if (alloyCount >= 12 && copperCount >= 2) {
            // Тир 2: 25 минут (12 сплавов + 2 медных слитка) -> +30% к форсажу
            tier = 1;
            durationTicks = 25 * 60 * 20;
            deductItem(player, "andesite_alloy", 12);
            deductItem(player, "copper_ingot", 2);
        } else if (alloyCount >= 4) {
            // Тир 1: 15 минут (4 сплава) -> базовая калибровка
            tier = 0;
            durationTicks = 15 * 60 * 20;
            deductItem(player, "andesite_alloy", 4);
        } else {
            // Недостаточно ресурсов
            serverPlayer.serverLevel().playSound(null, player.getX(), player.getY() + 1.0, player.getZ(),
                    SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.6f, 0.6f);
            return;
        }

        // Применяем бафф калибровки соответствующего тира
        player.addEffect(new MobEffectInstance(ModEffects.CALIBRATED_SERVOS.get(), durationTicks, tier, false, true, true));

        // Звуки и визуал продувки сервоприводов
        ServerLevel level = serverPlayer.serverLevel();
        double px = player.getX();
        double py = player.getY() + 1.1;
        double pz = player.getZ();

        if (tier == 2) {
            level.playSound(null, px, py, pz, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.5f);
            level.playSound(null, px, py, pz, SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0f, 1.4f);
            level.playSound(null, px, py, pz, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.9f, 0.7f);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz, 25, 0.4, 0.4, 0.4, 0.08);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 20, 0.5, 0.5, 0.5, 0.15);
            level.sendParticles(ParticleTypes.FLAME, px, py, pz, 10, 0.3, 0.3, 0.3, 0.05);
        } else if (tier == 1) {
            level.playSound(null, px, py, pz, SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.9f, 1.3f);
            level.playSound(null, px, py, pz, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 0.8f);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz, 18, 0.35, 0.35, 0.35, 0.06);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 15, 0.4, 0.4, 0.4, 0.12);
        } else {
            level.playSound(null, px, py, pz, SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8f, 1.1f);
            level.playSound(null, px, py, pz, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7f, 0.9f);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz, 12, 0.3, 0.3, 0.3, 0.05);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 10, 0.3, 0.3, 0.3, 0.08);
        }

        player.swing(event.getHand(), true);
    }

    public static int countItem(Player player, String pathSubstring) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (rl != null && rl.getPath().contains(pathSubstring)) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }

    public static ItemStack findItem(Player player, String pathSubstring) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (rl != null && rl.getPath().contains(pathSubstring)) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static void deductItem(Player player, String pathSubstring, int amount) {
        int remaining = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (rl != null && rl.getPath().contains(pathSubstring)) {
                    int take = Math.min(stack.getCount(), remaining);
                    stack.shrink(take);
                    remaining -= take;
                    if (remaining <= 0) break;
                }
            }
        }
    }

    // =========================================================================
    // ⚡ 4. МЕХАНИЧЕСКИЙ ОВЕРКЛОКИНГ (КИСЛОТНЫЙ ФОРСАЖ СКОРОСТИ АТАКИ И ВЫСТРЕЛОВ)
    // =========================================================================
    private static void handleMechCrouchCharging(ServerPlayer player) {
        CompoundTag pData = player.getPersistentData();
        int chargeTicks = pData.getInt("mech_charge_ticks") + 1;
        pData.putInt("mech_charge_ticks", chargeTicks);

        long gameTime = player.level().getGameTime();
        long cdUntil = pData.getLong("mech_overclock_cooldown");
        if (gameTime < cdUntil) return;

        // Каждые 25 тиков приседа растет давление в сервоприводах
        if (chargeTicks % 25 == 0 && chargeTicks <= 100) {
            pData.putInt("mech_hold_ticks", 200); // 10 секунд удержания
            int bars = chargeTicks / 25;
            float pitch = 0.8f + (bars * 0.25f);
            player.serverLevel().playSound(null, player.getX(), player.getY() + 1.0, player.getZ(),
                    SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.8f, pitch);
            player.serverLevel().sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.8, player.getZ(), 8, 0.2, 0.2, 0.2, 0.02);
        }
    }

    /**
     * Активация форсажа при нанесении удара во время механического перегруза
     */
    public static void triggerKineticOverclock(ServerPlayer player) {
        CompoundTag pData = player.getPersistentData();
        int chargeTicks = pData.getInt("mech_charge_ticks");
        if (chargeTicks < 25) return;

        int bars = Math.min(4, chargeTicks / 25);
        pData.putInt("mech_charge_ticks", 0);
        pData.putInt("mech_hold_ticks", 0);

        long gameTime = player.level().getGameTime();

        // Длительность форсажа: ВСЕГДА ровно 5 секунд (100 тиков)!
        int duration = 100;

        // Кулдаун после спуска: 5 сек действия + bars * 5 сек отката (5с для тира 1, 10с для тира 2, 15с для тира 3, 20с для тира 4)
        int cdTicks = bars * 100;
        pData.putLong("mech_overclock_cooldown", gameTime + duration + cdTicks);
        pData.putInt("mech_max_cooldown", cdTicks);

        boolean hasMastery = pData.getBoolean("MatrixOverclockMastery");
        boolean hasCalibrated = player.hasEffect(ModEffects.CALIBRATED_SERVOS.get());
        int servoAmp = hasCalibrated ? player.getEffect(ModEffects.CALIBRATED_SERVOS.get()).getAmplifier() : -1;

        player.addEffect(new MobEffectInstance(ModEffects.KINETIC_OVERCLOCK.get(), duration, bars - 1, false, true, true));

        // Перк Тир 3 «Сброс Пара»: мощный выхлоп под высоким давлением с фронтальным отбрасыванием
        if (servoAmp >= 2) {
            triggerSteamVentBurst(player, 8.0f);
        }

        // Начисление перегрузки (при эндгейм-матрицах = 0, при калиброванных сервоприводах - в 4 раза меньше!)
        float loadAdded = hasMastery ? 0.0f : ((bars * 0.5f) * (hasCalibrated ? 0.25f : 1.0f));

        if (loadAdded > 0.0f) {
            float curMech = pData.contains("mech_caster_load", 5) ? pData.getFloat("mech_caster_load") : (float) pData.getInt("mech_caster_load");
            pData.putFloat("mech_caster_load", curMech + loadAdded);
            pData.putInt("mech_shock_timer", 100);
        }

        ServerLevel level = player.serverLevel();
        double px = player.getX();
        double py = player.getY() + 1.1;
        double pz = player.getZ();

        level.playSound(null, px, py, pz, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.9f, 1.6f);
        level.playSound(null, px, py, pz, SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.PLAYERS, 1.0f, 1.2f);
        level.sendParticles(ParticleTypes.FLAME, px, py, pz, 15, 0.3, 0.3, 0.3, 0.08);
    }

    /**
     * Перк «Сброс Пара» (Steam Vent Burst) — направленный выхлоп раскалённого пара под давлением
     */
    public static void triggerSteamVentBurst(ServerPlayer player, float damage) {
        ServerLevel level = player.serverLevel();
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        net.minecraft.world.phys.Vec3 eyePos = player.getEyePosition();

        level.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7f, 1.8f);
        level.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.6f);

        // Конус пара перед игроком
        for (int i = 0; i < 22; i++) {
            double spread = 0.35;
            double px = eyePos.x + look.x * (1.0 + Math.random() * 3.5) + (Math.random() - 0.5) * spread * 3;
            double py = eyePos.y + look.y * (1.0 + Math.random() * 3.5) + (Math.random() - 0.5) * spread * 2;
            double pz = eyePos.z + look.z * (1.0 + Math.random() * 3.5) + (Math.random() - 0.5) * spread * 3;
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz, 1, 0, 0.04, 0, 0.05);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.FLAME, px, py, pz, 1, 0, 0.02, 0, 0.04);
            }
        }

        net.minecraft.world.phys.AABB coneBox = player.getBoundingBox().inflate(4.5);
        List<net.minecraft.world.entity.LivingEntity> entities = level.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class, coneBox, e -> e != player && e.isAlive()
        );

        for (net.minecraft.world.entity.LivingEntity target : entities) {
            net.minecraft.world.phys.Vec3 toTarget = target.position().subtract(player.position()).normalize();
            double dot = look.dot(toTarget);
            if (dot > 0.45) { // Конус ~65 градусов спереди
                target.hurt(player.damageSources().mobAttack(player), damage);
                target.setRemainingFireTicks(70);
                target.setDeltaMovement(toTarget.x * 0.95, 0.35, toTarget.z * 0.95);
                target.hurtMarked = true;
            }
        }
    }
}
