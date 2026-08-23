package com.example.arcanebridge.logic;

import com.example.arcanebridge.network.NetworkHandler;
import com.example.arcanebridge.network.PacketSyncControlGlitch;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Random;

public class ResonancePenalties {
    private static final Random random = new Random();

    public static void apply(Player player, float stability, boolean mech, boolean arcane, boolean ele) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) return;
        ServerLevel level = (ServerLevel) player.level();
        CompoundTag pData = player.getPersistentData();
        long currentTick = level.getGameTime();

        // =========================================================================
        // ⚙️ ТЕХНОГЕННАЯ ПЕРЕГРУЗКА (MECHANICAL)
        // =========================================================================
        if (mech) {
            // --- ТИР 1 (< 75%): Люфт прицела + Редкое заклинивание (1 раз в 3 минуты) ---
            if (stability <= 75.0f) {
                if (random.nextInt(100) < 5) {
                    player.swing(player.getUsedItemHand(), true);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.4f, 0.6f);
                }

                // Кулдаун 3 минуты (3600 тиков) с 30% шансом
                long lastJam = pData.getLong("ArcaneLastOverloadJam");
                if (currentTick - lastJam >= 3600) {
                    pData.putLong("ArcaneLastOverloadJam", currentTick);
                    if (random.nextInt(100) < 30) {
                        ArmorCalibrationEngine.jamRandomArmorPiece(serverPlayer);
                    }
                }
            }

            // --- ТИР 2 (< 50%): Выхлоп пара + Сброс импульса + Сбой WASD ---
            if (stability <= 50.0f) {
                if (player.isSprinting() && player.tickCount % 40 == 0) {
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY(), player.getZ(), 8, 0.1, 0.1, 0.1, 0.05);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 0.7f);
                    player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
                    player.hurtMarked = true;

                    long lastGlitch = pData.getLong("ArcaneLastGlitch");
                    if (currentTick - lastGlitch >= 600) {
                        pData.putLong("ArcaneLastGlitch", currentTick);
                        NetworkHandler.sendToPlayer(serverPlayer, new PacketSyncControlGlitch(50));
                    }
                }
            }

            // --- ТИР 3 (< 25%): Гальванический пробой током ---
            if (stability <= 25.0f && random.nextInt(100) < 3) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 12, 0.4, 0.5, 0.4, 0.1);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.4f, 1.8f);
                player.hurt(player.damageSources().magic(), 1.0f);
            }
        }

        // =========================================================================
        // 🔮 АРКАННАЯ ПЕРЕГРУЗКА (ARCANE)
        // =========================================================================
        if (arcane) {
            MobEffect shrinkEffect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("hexcasting", "shrink_grid"));

            if (stability <= 75.0f && shrinkEffect != null && !player.hasEffect(shrinkEffect)) {
                player.addEffect(new MobEffectInstance(shrinkEffect, 160, 0, false, false, true));
            }

            if (stability <= 50.0f && player.tickCount % 100 == 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.2f);
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 1, player.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
            }

            if (stability <= 25.0f && random.nextInt(100) < 3) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.8f, 0.7f);
                level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 15, 0.5, 0.5, 0.5, 0.1);
                player.hurt(player.damageSources().magic(), 1.0f);
            }
        }

        // =========================================================================
        // 🔥 СТИХИЙНАЯ ПЕРЕГРУЗКА (ELEMENTAL)
        // =========================================================================
        if (ele) {
            if (stability <= 75.0f && player.tickCount % 15 == 0) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.1, player.getZ(), 2, 0.1, 0.1, 0.1, 0.01);
            }

            if (player.isInWater() && player.tickCount % 20 == 0) {
                level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1, player.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4f, 1.2f);
                player.hurt(player.damageSources().drown(), 1.0f);
            }

            if (stability <= 50.0f && random.nextInt(100) < 4) {
                if (random.nextBoolean()) {
                    player.setSecondsOnFire(2);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 1.0f);
                } else {
                    player.setTicksFrozen(300);
                    level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1, player.getZ(), 15, 0.3, 0.3, 0.3, 0.05);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.6f, 1.0f);
                }
            }

            if (stability <= 25.0f && player.tickCount % 240 == 0) {
                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
                if (lightning != null) {
                    double ox = (random.nextBoolean() ? 1 : -1) * (4 + random.nextDouble() * 3);
                    double oz = (random.nextBoolean() ? 1 : -1) * (4 + random.nextDouble() * 3);
                    lightning.moveTo(player.getX() + ox, player.getY(), player.getZ() + oz);
                    lightning.setVisualOnly(true);
                    level.addFreshEntity(lightning);
                }
            }
        }

        // =========================================================================
        // 🌀 КРИТИЧЕСКАЯ ИМПЛОЗИЯ СИНГУЛЯРНОСТИ (< 25%)
        // =========================================================================
        if (stability <= 25.0f) {
            if (player.tickCount % 220 == 0) {
                triggerSafeImplosion(serverPlayer, level);
            }
        }
    }

    public static void triggerSafeImplosion(ServerPlayer player, ServerLevel level) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        level.playSound(null, px, py, pz, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.4f);
        level.playSound(null, px, py, pz, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.6f, 1.6f);

        level.sendParticles(ParticleTypes.SONIC_BOOM, px, py + 1.0, pz, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, px, py + 1.0, pz, 35, 1.2, 1.2, 1.2, 0.1);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py + 1.0, pz, 20, 0.8, 0.8, 0.8, 0.15);

        player.hurt(player.damageSources().magic(), 1.5f);

        player.setDeltaMovement(player.getDeltaMovement().x, 0.45D, player.getDeltaMovement().z);
        player.hurtMarked = true;
        player.hasImpulse = true;

        AABB suctionArea = new AABB(player.blockPosition()).inflate(10.0D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, suctionArea, e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            Vec3 dir = new Vec3(px - target.getX(), (py + 0.3) - target.getY(), pz - target.getZ());
            double dist = dir.length();

            if (dist > 0.4D) {
                Vec3 pullMotion = dir.normalize().scale(Math.min(1.4D, 0.5D + (dist * 0.12D)));
                target.setDeltaMovement(pullMotion.x, 0.32D, pullMotion.z);
                target.hurtMarked = true;
                target.hasImpulse = true;
            }
        }
    }
}