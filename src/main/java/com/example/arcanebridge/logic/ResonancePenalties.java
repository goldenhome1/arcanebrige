package com.example.arcanebridge.logic;

import com.example.arcanebridge.network.NetworkHandler;
import com.example.arcanebridge.network.PacketSyncControlGlitch;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
            // --- ТИР 1 (<= 85%): Люфт прицела + Утомление мышц ---
            if (stability <= 85.0f) {
                if (random.nextInt(100) < 5) {
                    player.swing(player.getUsedItemHand(), true);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.4f, 0.6f);
                }
                if (player.isSprinting() && player.tickCount % 20 == 0) {
                    player.causeFoodExhaustion(0.35f);
                }
            }

            // --- ТИР 2 (<= 70%): Паровой срыв + Сбой WASD ---
            if (stability <= 70.0f) {
                if (player.isSprinting() && player.tickCount % 160 == 0) {
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY(), player.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 0.7f);
                    player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
                    player.hurtMarked = true;
                    player.hurt(player.damageSources().hotFloor(), 1.0f);

                    boolean hasCalibrated = player.hasEffect(com.example.arcanebridge.registry.ModEffects.CALIBRATED_SERVOS.get())
                            || pData.getBoolean("MatrixOverclockMastery");
                    if (!hasCalibrated) {
                        long lastGlitch = pData.getLong("ArcaneLastGlitch");
                        if (currentTick - lastGlitch >= 600) {
                            pData.putLong("ArcaneLastGlitch", currentTick);
                            NetworkHandler.sendToPlayer(serverPlayer, new PacketSyncControlGlitch(40));
                        }
                    }
                }
            }

            // --- ТИР 3 (<= 50%): Заклинивание сервоприводов (Jammed) ---
            if (stability <= 50.0f) {
                boolean hasCalibrated = player.hasEffect(com.example.arcanebridge.registry.ModEffects.CALIBRATED_SERVOS.get())
                        || pData.getBoolean("MatrixOverclockMastery");
                if (!hasCalibrated) {
                    long lastJam = pData.getLong("ArcaneLastOverloadJam");
                    if (currentTick - lastJam >= 1600) { // Кулдаун 80 секунд
                        pData.putLong("ArcaneLastOverloadJam", currentTick);
                        if (random.nextInt(100) < 35) {
                            ArmorCalibrationEngine.jamRandomArmorPiece(serverPlayer);
                            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0, false, false, true));
                        }
                    }
                }
            }

            // --- ТИР 4 (<= 35%): Магнитный перегруз & Пробой током ---
            if (stability <= 35.0f) {
                boolean hasCalibrated = player.hasEffect(com.example.arcanebridge.registry.ModEffects.CALIBRATED_SERVOS.get())
                        || pData.getBoolean("MatrixOverclockMastery");
                if (!hasCalibrated) {
                    long lastJamTier4 = pData.getLong("ArcaneLastOverloadJamT4");
                    if (currentTick - lastJamTier4 >= 600) {
                        pData.putLong("ArcaneLastOverloadJamT4", currentTick);
                        if (random.nextInt(100) < 40) {
                            ArmorCalibrationEngine.jamRandomArmorPiece(serverPlayer);
                        }
                    }
                }

                if (random.nextInt(100) < 5) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 14, 0.4, 0.5, 0.4, 0.1);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5f, 1.8f);
                    player.hurt(player.damageSources().lightningBolt(), 1.5f);
                }
            }

            // --- ТИР 5 (<= 20%): Разрыв парового котла ---
            if (stability <= 20.0f && player.tickCount % 240 == 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.8f);
                level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1.0, player.getZ(), 2, 0.1, 0.1, 0.1, 0.05);
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.6, 0.6, 0.6, 0.08);

                player.hurt(player.damageSources().explosion(null, null), 4.0f);
                player.setDeltaMovement(player.getDeltaMovement().x, 0.4D, player.getDeltaMovement().z);
                player.hurtMarked = true;

                // Ударная волна раскидывает мобов вокруг
                AABB blastArea = new AABB(player.blockPosition()).inflate(6.0D);
                List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, blastArea, e -> e != player && e.isAlive());
                for (LivingEntity enemy : enemies) {
                    Vec3 push = enemy.position().subtract(player.position()).normalize().scale(1.2D);
                    enemy.setDeltaMovement(push.x, 0.35D, push.z);
                    enemy.hurtMarked = true;
                    enemy.hurt(player.damageSources().explosion(null, null), 6.0f);
                }
            }
        }

        // =========================================================================
        // 🔮 АРКАННАЯ ПЕРЕГРУЗКА (ARCANE)
        // =========================================================================
        if (arcane) {
            // --- ТИР 1 (<= 85%): Звон кристаллов + Сжатие сетки Hex ---
            if (stability <= 85.0f) {
                MobEffect shrinkEffect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("hexcasting", "shrink_grid"));
                if (shrinkEffect != null && !player.hasEffect(shrinkEffect)) {
                    player.addEffect(new MobEffectInstance(shrinkEffect, 160, 0, false, false, true));
                }
                if (player.tickCount % 140 == 0) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 1.3f);
                }
            }

            // --- ТИР 2 (<= 70%): Эфирная утечка (Сжигание опыта) ---
            if (stability <= 70.0f && player.tickCount % 80 == 0) {
                if (serverPlayer.totalExperience > 0) {
                    serverPlayer.giveExperiencePoints(-2);
                    level.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 1.2, player.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.5f, 1.5f);
                }
            }

            // --- ТИР 3 (<= 50%): Эфирный дрифт, Квантовый рассинхрон и Астральный заклиниватель ---
            if (stability <= 50.0f) {
                // 1. Потеря трения и пространственный дрифт при беге
                if (player.isSprinting() && player.tickCount % 35 == 0) {
                    Vec3 motion = player.getDeltaMovement();
                    double drift = (random.nextDouble() - 0.5) * 0.3;
                    player.setDeltaMovement(motion.x * 1.32D + drift, motion.y, motion.z * 1.32D - drift);
                    player.hurtMarked = true;
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 0.2, player.getZ(), 8, 0.2, 0.1, 0.2, 0.05);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.45f, 1.6f);
                }

                // 2. Квантовый рассинхрон (микро-телепорт с дезориентацией)
                if (player.tickCount % 100 == 0 && random.nextInt(100) < 35) {
                    double ox = (random.nextDouble() - 0.5) * 6.0;
                    double oz = (random.nextDouble() - 0.5) * 6.0;
                    double targetX = player.getX() + ox;
                    double targetY = player.getY() + 0.1;
                    double targetZ = player.getZ() + oz;

                    level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.4, 0.5, 0.4, 0.15);
                    level.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 1.0, player.getZ(), 15, 0.3, 0.4, 0.3, 0.1);
                    serverPlayer.teleportTo(targetX, targetY, targetZ);
                    level.playSound(null, targetX, targetY, targetZ, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.85f, 1.2f);
                    level.playSound(null, targetX, targetY, targetZ, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.35f, 1.8f);
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 25, 0, false, false, true));
                }

                // 3. Астральный заклиниватель (Resonance Lockout вооружения)
                long lastLockout = pData.getLong("ArcaneLastSpellLockout");
                if (currentTick - lastLockout >= 1400) { // Кулдаун 70 секунд
                    pData.putLong("ArcaneLastSpellLockout", currentTick);
                    if (random.nextInt(100) < 40) {
                        ItemStack held = player.getMainHandItem();
                        if (!held.isEmpty()) {
                            player.getCooldowns().addCooldown(held.getItem(), 70); // 3.5 секунды кулдауна
                        }
                        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 1, false, false, true));
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.9f, 0.8f);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7f, 1.9f);
                        level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.2, player.getZ(), 1, 0, 0, 0, 0);
                    }
                }
                // 4. Пассивный выброс эфирных щупалец со спины (раз в 9 секунд)
                if (!player.hasEffect(com.example.arcanebridge.registry.ModEffects.CRYSTALLINE_SATIATION.get())
                        && !pData.getBoolean("MatrixTendrilMastery")) {
                    long lastPassiveTendril = pData.getLong("ArcaneLastPassiveTendril");
                    if (currentTick - lastPassiveTendril >= 180) {
                        pData.putLong("ArcaneLastPassiveTendril", currentTick);
                        triggerTendrilWhip(serverPlayer, level, false);
                    }
                }
            }

            // --- ТИР 4 (<= 35%): Кристаллизация плоти и Гравитационный провал ---
            if (stability <= 35.0f) {
                if (!player.hasEffect(MobEffects.WEAKNESS)) {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1, false, false, true));
                }
                if (!player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 0, false, false, true));
                }
                if (player.tickCount % 80 == 0 && random.nextInt(100) < 35) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.9f, 0.6f);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.8f, 1.2f);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 18, 0.4, 0.5, 0.4, 0.12);
                    player.hurt(player.damageSources().magic(), 2.5f);
                    player.setDeltaMovement(player.getDeltaMovement().x, random.nextBoolean() ? 0.35D : -0.4D, player.getDeltaMovement().z);
                    player.hurtMarked = true;
                }
            }

            // --- ТИР 5 (<= 20%): Эфирный разрыв реальности ---
            if (stability <= 20.0f && player.tickCount % 180 == 0) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.8, 0.8, 0.8, 0.15);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.7f, 1.8f);
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false, true));
                player.hurt(player.damageSources().magic(), 3.5f);
            }
        }

        // =========================================================================
        // 🔥 СТИХИЙНАЯ ПЕРЕГРУЗКА (ELEMENTAL)
        // =========================================================================
        if (ele) {
            // --- ТИР 1 (<= 85%): Стихийное эхо (Пламя душ под ногами) ---
            if (stability <= 85.0f && player.tickCount % 15 == 0) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.1, player.getZ(), 2, 0.1, 0.1, 0.1, 0.01);
            }

            // --- ТИР 2 (<= 70%): Термошок оружия ---
            if (stability <= 70.0f && player.tickCount % 120 == 0) {
                ItemStack mainStack = player.getMainHandItem();
                if (!mainStack.isEmpty() && mainStack.isDamageableItem()) {
                    mainStack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
                    level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4f, 1.5f);
                }
            }

            // --- ТИР 3 (<= 50%): Спонтанный огонь, Кипение в воде и Пассивные щупальца ---
            if (stability <= 50.0f) {
                if ((player.isInWater() || player.isInWaterRainOrBubble()) && player.tickCount % 25 == 0) {
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY() + 0.8, player.getZ(), 14, 0.35, 0.35, 0.35, 0.04);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7f, 1.1f);
                    player.hurt(player.damageSources().hotFloor(), 2.0f);
                } else if (!player.isInWater() && player.tickCount % 80 == 0 && random.nextInt(100) < 40) {
                    player.setSecondsOnFire(3);
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.3, 0.4, 0.3, 0.05);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.75f, 1.1f);
                }

                if (player.tickCount % 100 == 0) {
                    ItemStack mainStack = player.getMainHandItem();
                    if (!mainStack.isEmpty() && mainStack.isDamageableItem()) {
                        mainStack.hurtAndBreak(2, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
                        level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.2, 0.2, 0.2, 0.03);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 1.4f);
                    }
                }

                // Пассивный выброс стихийных щупалец со спины
                if (!player.hasEffect(com.example.arcanebridge.registry.ModEffects.CRYSTALLINE_SATIATION.get())
                        && !pData.getBoolean("MatrixTendrilMastery")) {
                    long lastPassiveEle = pData.getLong("EleLastPassiveTendril");
                    if (currentTick - lastPassiveEle >= 180) {
                        pData.putLong("EleLastPassiveTendril", currentTick);
                        triggerTendrilWhip(serverPlayer, level, false);
                    }
                }
            }

            // --- ТИР 4 (<= 35%): Крио-стаз и Молнии ---
            if (stability <= 35.0f) {
                if (player.tickCount % 140 == 0 && random.nextInt(100) < 35) {
                    player.setTicksFrozen(100); // 5 секунд замерзания
                    level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1, player.getZ(), 20, 0.4, 0.4, 0.4, 0.08);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.7f, 1.0f);
                }

                if (player.tickCount % 200 == 0) {
                    LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
                    if (lightning != null) {
                        double ox = (random.nextBoolean() ? 1 : -1) * (3.0 + random.nextDouble() * 3.0);
                        double oz = (random.nextBoolean() ? 1 : -1) * (3.0 + random.nextDouble() * 3.0);
                        lightning.moveTo(player.getX() + ox, player.getY(), player.getZ() + oz);
                        lightning.setVisualOnly(false);
                        level.addFreshEntity(lightning);
                    }
                }
            }

            // --- ТИР 5 (<= 20%): Сверхновая ---
            if (stability <= 20.0f && player.tickCount % 180 == 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 0.9f);
                level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 35, 0.8, 0.8, 0.8, 0.15);
                player.setSecondsOnFire(5);
                player.hurt(player.damageSources().onFire(), 4.0f);

                // Износ брони от сверхновой
                for (ItemStack armor : player.getArmorSlots()) {
                    if (!armor.isEmpty() && armor.isDamageableItem()) {
                        armor.hurtAndBreak(5, player, p -> p.broadcastBreakEvent(EquipmentSlot.CHEST));
                    }
                }
            }
        }

        // =========================================================================
        // 🌀 АВАРИЙНАЯ ДЕТОНАЦИЯ СИНГУЛЯРНОСТИ (<= 20%)
        // =========================================================================
        if (stability <= 20.0f) {
            if (player.tickCount % 240 == 0) {
                triggerSafeImplosion(serverPlayer, level);
            }
        }
    }

    public static void triggerSafeImplosion(ServerPlayer player, ServerLevel level) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        level.playSound(null, px, py, pz, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9f, 1.2f);
        level.playSound(null, px, py, pz, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1.4f);

        level.sendParticles(ParticleTypes.SONIC_BOOM, px, py + 1.0, pz, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, px, py + 1.0, pz, 35, 1.2, 1.2, 1.2, 0.1);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py + 1.0, pz, 20, 0.8, 0.8, 0.8, 0.15);

        // Урон игроку от отдачи
        player.hurt(player.damageSources().magic(), 2.0f);
        player.setDeltaMovement(player.getDeltaMovement().x, 0.4D, player.getDeltaMovement().z);
        player.hurtMarked = true;
        player.hasImpulse = true;

        // Взрывная волна ОТТАЛКИВАЕТ всех мобов наружу (спасая игрока) и наносит им урон!
        AABB blastArea = new AABB(player.blockPosition()).inflate(8.0D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, blastArea, e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            Vec3 dir = new Vec3(target.getX() - px, (target.getY() + 0.2) - py, target.getZ() - pz);
            double dist = dir.length();

            if (dist > 0.1D) {
                Vec3 pushMotion = dir.normalize().scale(Math.min(1.8D, 0.8D + (1.0D / Math.max(1.0D, dist))));
                target.setDeltaMovement(pushMotion.x, 0.45D, pushMotion.z);
                target.hurtMarked = true;
                target.hasImpulse = true;
                target.hurt(level.damageSources().magic(), 5.0f);
            }
        }
    }

    /**
     * Выброс пространственных щупалец маны (пассивный со спины или намеренный при Shift+Атаке).
     * Намеренный всплеск дает мощный урон по площади, но НАКАЗЫВАЕТ игрока дополнительным перегрузом (+2 к шоку) и отдачей!
     */
    public static void triggerTendrilWhip(ServerPlayer player, ServerLevel level, boolean intentionalSurge) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        double yawRad = Math.toRadians(player.yBodyRot);
        double backX = Math.sin(yawRad);
        double backZ = -Math.cos(yawRad);

        float damage = intentionalSurge ? 8.0f : 5.5f;
        double range = intentionalSurge ? 4.8D : 3.6D;

        AABB hitBox = player.getBoundingBox().inflate(range);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != player && e.isAlive());

        Vec3 lookVec = player.getLookAngle();

        for (LivingEntity target : targets) {
            Vec3 diff = target.position().subtract(player.position());
            double dist = diff.length();
            if (dist < 0.1D) continue;

            Vec3 dirNorm = diff.normalize();

            // Для пассивного взмаха щупалец со спины: поражает только цели со спины и флангов (dot <= 0.35)
            // Для намеренного выплеска (Shift + Attack): разрывает все цели в радиусе!
            if (intentionalSurge || dirNorm.dot(lookVec) <= 0.35D) {
                target.hurt(player.damageSources().magic(), damage);
                Vec3 knock = dirNorm.scale(intentionalSurge ? 1.4D : 0.9D);
                target.setDeltaMovement(knock.x, 0.35D, knock.z);
                target.hurtMarked = true;
                target.hasImpulse = true;
            }
        }

        if (intentionalSurge) {
            // Эффекты контролируемого, но дикого и разрушительного выброса
            level.playSound(null, px, py + 1.0, pz, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1.5f);
            level.playSound(null, px, py + 1.0, pz, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0f, 0.7f);
            level.sendParticles(ParticleTypes.SONIC_BOOM, px, py + 1.1, pz, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, px, py + 1.0, pz, 30, 0.8, 0.8, 0.8, 0.12);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, px, py + 1.0, pz, 35, 0.9, 0.9, 0.9, 0.15);

            // Наказание игрока за использование дикой нестабильной энергии:
            // 1. Дополнительный перегруз (+2 к текущей нагрузке заклинателя) и продление таймера шока до 120 тиков (6 секунд)!
            CompoundTag pData = player.getPersistentData();
            int currentArcane = pData.getInt("arcane_caster_load");
            int currentEle = pData.getInt("ele_caster_load");

            if (currentEle > currentArcane) {
                pData.putInt("ele_caster_load", currentEle + 2);
                pData.putInt("ele_shock_timer", 120);
            } else {
                pData.putInt("arcane_caster_load", Math.max(1, currentArcane + 2));
                pData.putInt("arcane_shock_timer", 120);
            }

            // 2. Отдача по телу от нестабильной магии (1.5 урона)
            player.hurt(player.damageSources().magic(), 1.5f);
            player.hurtMarked = true;
        } else {
            // Пассивный взмах щупалец со спины
            level.playSound(null, px, py + 1.0, pz, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.3f);
            level.playSound(null, px, py + 1.0, pz, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8f, 1.5f);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, px + backX * 0.8, py + 1.1, pz + backZ * 0.8, 15, 0.4, 0.4, 0.4, 0.05);
        }
    }
}