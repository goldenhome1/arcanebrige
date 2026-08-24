package com.example.arcanebridge.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class ArcaneGuideEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

            private static final EntityDataAccessor<Integer> ANIM_STATE =
            SynchedEntityData.defineId(ArcaneGuideEntity.class, EntityDataSerializers.INT);
        private static final EntityDataAccessor<Integer> NIGHT_SHIELD_TICKS =
            SynchedEntityData.defineId(ArcaneGuideEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION_TICKS =
            SynchedEntityData.defineId(ArcaneGuideEntity.class, EntityDataSerializers.INT);

    public static final int STATE_IDLE = 0;
    public static final int STATE_CHARGE = 1;
    public static final int STATE_GREETING = 2;
    public static final int STATE_ANALYZE = 3;
    public static final int STATE_PONDER = 4;
    public static final int STATE_EXPLAIN = 5;
        public static final int STATE_CALIBRATE = 6;
    public static final int STATE_SHIELD_NIGHT = 7;
    public static final int STATE_MATERIALIZE = 8;
    public static final int STATE_DEMATERIALIZE = 9;

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation CHARGE_ANIM = RawAnimation.begin().thenPlay("charge");
    private static final RawAnimation SHIELD_LOOP_ANIM = RawAnimation.begin().thenLoop("shield_night");
    private static final RawAnimation GREETING_ANIM = RawAnimation.begin().thenPlay("greeting");
    private static final RawAnimation ANALYZE_ANIM = RawAnimation.begin().thenLoop("analyze");
    private static final RawAnimation PONDER_ANIM = RawAnimation.begin().thenPlay("ponder");
    private static final RawAnimation EXPLAIN_ANIM = RawAnimation.begin().thenLoop("explain");
    private static final RawAnimation CALIBRATE_ANIM = RawAnimation.begin().thenPlay("calibrate_eye");

        private static final double SHIELD_RADIUS = 6.5D;
    private static final double TARGET_HOVER_HEIGHT = 1.0D;

    private int stateTimer = 0;
    private int idleFlavorCooldown = 200;

    public ArcaneGuideEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

            @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIM_STATE, STATE_IDLE);
        this.entityData.define(NIGHT_SHIELD_TICKS, 0);
        this.entityData.define(ACTION_TICKS, 0);
    }

    public int getNightShieldTicks() {
        return this.entityData.get(NIGHT_SHIELD_TICKS);
    }

    public int getActionTicks() {
        return this.entityData.get(ACTION_TICKS);
    }

    public int getAnimState() {
        return this.entityData.get(ANIM_STATE);
    }

    public void setAnimState(int state, int durationTicks) {
        this.entityData.set(ANIM_STATE, state);
        this.entityData.set(ACTION_TICKS, durationTicks);
        this.stateTimer = durationTicks;
    }

        public boolean isCasting() {
        return this.getAnimState() == STATE_CHARGE;
    }

        public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public void performStructureScan(net.minecraft.server.level.ServerPlayer player, String structureId) {
        this.setAnimState(STATE_CHARGE, 60);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 1.0F, 1.3F);

        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.resources.ResourceLocation structLoc = new net.minecraft.resources.ResourceLocation(structureId);
            var structRegistry = serverLevel.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
            var structure = structRegistry.get(structLoc);

            if (structure != null) {
                var holderSet = net.minecraft.core.HolderSet.direct(structRegistry.getHolderOrThrow(structRegistry.getResourceKey(structure).orElseThrow()));
                                // Увеличенный радиус поиска (250 чанков = 4000 блоков)
                var result = serverLevel.getChunkSource().getGenerator().findNearestMapStructure(
                        serverLevel, holderSet, player.blockPosition(), 250, false
                );

                if (result != null) {
                    net.minecraft.core.BlockPos pos = result.getFirst();
                    int dist = (int) Math.sqrt(player.blockPosition().distSqr(pos));
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§6[Гид] §aСпектральный сигнал зафиксирован: §eX: " + pos.getX() + "§7, §eZ: " + pos.getZ() + " §7(~" + dist + " блоков)"
                    ));
                    return;
                }
            }
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§6[Гид] §cСигнал рассеялся. В радиусе 4000 блоков целевая структура не обнаружена."
            ));
        }
    }

        @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Убираем RandomLookAroundGoal и ставим 100% приоритет удержания взгляда на игроке
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10.0F, 1.0F));
    }

                    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        Level level = this.level();

        // 1. Shift + ПКМ: Сворачивание Гида в ядро
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                packIntoCore(player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // 2. Серверная проверка и оказание первой помощи
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            applyFirstAid(serverPlayer);
        }

        // 3. Безопасное открытие экрана диалога только на стороне клиента игрока
        if (level.isClientSide()) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
                com.example.arcanebridge.client.ClientGuiOpener.openGuideDialogue(this.getId());
            });
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private void applyFirstAid(net.minecraft.server.level.ServerPlayer player) {
        net.minecraft.world.effect.MobEffect bleeding = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS
                .getValue(new net.minecraft.resources.ResourceLocation("majruszsdifficulty", "bleeding"));

        boolean hasBleeding = bleeding != null && player.hasEffect(bleeding);
        boolean isCriticalHealth = player.getHealth() <= 6.0F;

        if (hasBleeding || isCriticalHealth) {
            if (hasBleeding) {
                player.removeEffect(bleeding);
            }
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.REGENERATION, 140, 0, false, true, true
            ));

            this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.8F, 1.2F);
            ((net.minecraft.server.level.ServerLevel) this.level()).sendParticles(
                    ParticleTypes.HEART, player.getX(), player.getY() + 1.2, player.getZ(), 5, 0.3, 0.3, 0.3, 0.1
            );
        }
    }

                @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Тикер синхронизированного таймера анимаций
            int actionRemaining = this.entityData.get(ACTION_TICKS);
            if (actionRemaining > 0) {
                this.entityData.set(ACTION_TICKS, actionRemaining - 1);
            }

            // Обработка дематериализации перед удалением
            if (this.dematerializeTimer > 0) {
                this.dematerializeTimer--;
                if (this.dematerializeTimer <= 0) {
                    finishPacking();
                }
                return; // Полностью блокируем ночной режим на время сворачивания
            }

            // Завершение фазы материализации
            if (this.getAnimState() == STATE_MATERIALIZE) {
                if (this.stateTimer > 0) {
                    this.stateTimer--;
                    if (this.stateTimer <= 0) {
                        this.setAnimState(STATE_GREETING, 45);
                    }
                }
                return;
            }

            boolean isNightTime = this.level().isNight();

            if (isNightTime) {
                if (this.getAnimState() != STATE_SHIELD_NIGHT) {
                    this.entityData.set(ANIM_STATE, STATE_SHIELD_NIGHT);
                    this.entityData.set(NIGHT_SHIELD_TICKS, 0);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 1.0F, 1.0F);
                }

                int currentTicks = this.entityData.get(NIGHT_SHIELD_TICKS);
                if (currentTicks < 60) {
                    this.entityData.set(NIGHT_SHIELD_TICKS, currentTicks + 1);
                }

                // Динамический контроль высоты: 1.0 блок над твердой поверхностью
                double actualGroundY = findGroundY();
                double targetY = actualGroundY + TARGET_HOVER_HEIGHT;
                double diffY = targetY - this.getY();

                // Плавная стабилизация по Y
                this.setDeltaMovement(this.getDeltaMovement().x * 0.5D, diffY * 0.2D, this.getDeltaMovement().z * 0.5D);
                this.hasImpulse = true;
                this.fallDistance = 0.0F;

                applyNightForceField();
            } else {
                if (this.getAnimState() == STATE_SHIELD_NIGHT) {
                    this.entityData.set(ANIM_STATE, STATE_IDLE);
                    this.entityData.set(NIGHT_SHIELD_TICKS, 0);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 1.0F, 1.0F);
                }

                if (this.stateTimer > 0) {
                    this.stateTimer--;
                    if (this.stateTimer <= 0) {
                        this.entityData.set(ANIM_STATE, STATE_IDLE);
                    }
                } else if (this.getAnimState() == STATE_IDLE) {
                    if (--this.idleFlavorCooldown <= 0) {
                        this.idleFlavorCooldown = 3000 + this.random.nextInt(3000);
                        if (this.random.nextBoolean()) {
                            this.setAnimState(STATE_PONDER, 60);
                        } else {
                            this.setAnimState(STATE_CALIBRATE, 30);
                        }
                        triggerAmbientBehavior();
                    }
                }
            }
        }
    }

                        private double findGroundY() {
        net.minecraft.core.BlockPos.MutableBlockPos pos = this.blockPosition().mutable();
        // Проверяем блоки под мобом вниз до 10 блоков
        for (int i = 0; i < 10; i++) {
            if (!this.level().getBlockState(pos).isAir()) {
                net.minecraft.world.phys.shapes.VoxelShape shape = this.level().getBlockState(pos).getCollisionShape(this.level(), pos);
                if (!shape.isEmpty()) {
                    return pos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
                }
                return pos.getY() + 1.0D;
            }
            pos.move(net.minecraft.core.Direction.DOWN);
        }
        return this.getY();
    }

        private void applyNightForceField() {
        AABB searchBox = this.getBoundingBox().inflate(SHIELD_RADIUS, 3.5D, SHIELD_RADIUS);
        // Фильтруем только враждебных мобов (Enemy) и снаряды, игнорируя служебные маркеры и невидимки
        List<Entity> entities = this.level().getEntities(this, searchBox,
                e -> (e instanceof Projectile || e instanceof net.minecraft.world.entity.monster.Enemy)
                        && e.isAlive() && !e.isSpectator() && !e.isInvisible());

        for (Entity entity : entities) {
            Vec3 diff = entity.position().subtract(this.position());
            double distSq = diff.x * diff.x + diff.z * diff.z;

            if (distSq < SHIELD_RADIUS * SHIELD_RADIUS && distSq > 0.0001D) {
                double dist = Math.sqrt(distSq);
                Vec3 pushDir = new Vec3(diff.x / dist, 0, diff.z / dist);
                double pushStrength = Math.max(0.65D, 1.3D - (dist / SHIELD_RADIUS));

                if (entity instanceof Projectile projectile) {
                    projectile.setDeltaMovement(pushDir.x * 1.1D, 0.35D, pushDir.z * 1.1D);
                    projectile.hurtMarked = true;
                } else {
                    entity.setDeltaMovement(pushDir.x * pushStrength, 0.22D, pushDir.z * pushStrength);
                    entity.hurtMarked = true;
                }

                if (this.random.nextInt(4) == 0) {
                    this.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            SoundEvents.SHIELD_BLOCK, SoundSource.NEUTRAL, 0.6F, 1.5F);
                }
            }
        }
    }

            private int dematerializeTimer = -1;
    private Player packingPlayer = null;

    private void packIntoCore(Player player) {
        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // Запуск дематериализации сверху вниз на 25 тиков
            this.setAnimState(STATE_DEMATERIALIZE, 25);
            this.dematerializeTimer = 25;
            this.packingPlayer = player;

            this.level().playSound(null, this.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.9F, 1.6F);
            this.level().playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.5F);
        }
    }

    private void finishPacking() {
        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel && this.packingPlayer != null) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 0.8, this.getZ(), 20, 0.2, 0.4, 0.2, 0.1);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 0.8, this.getZ(), 15, 0.2, 0.3, 0.2, 0.15);

            net.minecraft.world.item.Item coreItem = net.minecraftforge.registries.ForgeRegistries.ITEMS
                    .getValue(new net.minecraft.resources.ResourceLocation("arcane_bridge", "guide_core"));
            if (coreItem != null) {
                net.minecraft.world.item.ItemStack coreStack = new net.minecraft.world.item.ItemStack(coreItem);
                if (!this.packingPlayer.getInventory().add(coreStack)) {
                    this.packingPlayer.drop(coreStack, false);
                }
            }
            this.discard();
        }
    }

    private void triggerAmbientBehavior() {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        Player nearbyPlayer = this.level().getNearestPlayer(this, 8.0D);
        if (nearbyPlayer == null) return;

        String phrase;
        if (this.level().isRaining()) {
            phrase = "«Влага повышает электропроводность среды. Берегите узлы кинетики от окисления.»";
        } else if (this.level().getDayTime() % 24000L > 11500L && this.level().getDayTime() % 24000L < 13000L) {
            phrase = "«Фиксирую спад освещения. Эфирный фон дестабилизируется, готовлю контур защиты.»";
        } else {
            String[] randomThoughts = {
                    "«Калибровка спектрального сенсора завершена. Отклонений не зафиксировано.»",
                    "«Следите за стабильностью резонанса: диссонанс накапливается незаметно.»",
                    "«Инженерные конвейеры требуют чистоты подачи компонентов.»"
            };
            phrase = randomThoughts[this.random.nextInt(randomThoughts.length)];
        }

        nearbyPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6[Гид] §8" + phrase));
        serverLevel.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY() + 1.6, this.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 40, event -> {
            return switch (this.getAnimState()) {
                case STATE_SHIELD_NIGHT -> event.setAndContinue(SHIELD_LOOP_ANIM);
                case STATE_CHARGE -> event.setAndContinue(CHARGE_ANIM);
                case STATE_GREETING -> event.setAndContinue(GREETING_ANIM);
                case STATE_ANALYZE -> event.setAndContinue(ANALYZE_ANIM);
                case STATE_PONDER -> event.setAndContinue(PONDER_ANIM);
                case STATE_EXPLAIN -> event.setAndContinue(EXPLAIN_ANIM);
                case STATE_CALIBRATE -> event.setAndContinue(CALIBRATE_ANIM);
                default -> event.setAndContinue(IDLE_ANIM);
            };
        }));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        if (this.getAnimState() == STATE_SHIELD_NIGHT) {
            return distance < 16384.0D;
        }
        return super.shouldRenderAtSqrDistance(distance);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}