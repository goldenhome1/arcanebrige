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

    public static final int STATE_IDLE = 0;
    public static final int STATE_CHARGE = 1;
    public static final int STATE_GREETING = 2;
    public static final int STATE_ANALYZE = 3;
    public static final int STATE_PONDER = 4;
    public static final int STATE_EXPLAIN = 5;
    public static final int STATE_CALIBRATE = 6;
    public static final int STATE_SHIELD_NIGHT = 7;

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation CHARGE_ANIM = RawAnimation.begin().thenPlay("charge");
    private static final RawAnimation SHIELD_LOOP_ANIM = RawAnimation.begin().thenLoop("shield_night");
    private static final RawAnimation GREETING_ANIM = RawAnimation.begin().thenPlay("greeting");
    private static final RawAnimation ANALYZE_ANIM = RawAnimation.begin().thenLoop("analyze");
    private static final RawAnimation PONDER_ANIM = RawAnimation.begin().thenPlay("ponder");
    private static final RawAnimation EXPLAIN_ANIM = RawAnimation.begin().thenLoop("explain");
    private static final RawAnimation CALIBRATE_ANIM = RawAnimation.begin().thenPlay("calibrate_eye");

    private static final double SHIELD_RADIUS = 6.5D;
    private static final double HOVER_HEIGHT = 1.5D;

    private double groundY = Double.NaN;
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
    }

    public int getNightShieldTicks() {
        return this.entityData.get(NIGHT_SHIELD_TICKS);
    }

    public int getAnimState() {
        return this.entityData.get(ANIM_STATE);
    }

    public void setAnimState(int state, int durationTicks) {
        this.entityData.set(ANIM_STATE, state);
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

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

            @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        // Безопасное открытие экрана диалога только на стороне клиента игрока
        if (this.level().isClientSide()) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
                com.example.arcanebridge.client.ClientGuiOpener.openGuideDialogue(this.getId());
            });
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

            @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            boolean isNightTime = this.level().isNight();

            if (isNightTime) {
                if (this.getAnimState() != STATE_SHIELD_NIGHT) {
                    this.entityData.set(ANIM_STATE, STATE_SHIELD_NIGHT);
                    this.entityData.set(NIGHT_SHIELD_TICKS, 0);
                    this.groundY = this.getY();
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 1.0F, 1.0F);
                }

                int currentTicks = this.entityData.get(NIGHT_SHIELD_TICKS);
                if (currentTicks < 60) {
                    this.entityData.set(NIGHT_SHIELD_TICKS, currentTicks + 1);
                }

                if (Double.isNaN(this.groundY)) {
                    this.groundY = this.getY();
                }

                // Физический подъем сущности и ее хитбокса
                float progress = Math.min(1.0F, this.entityData.get(NIGHT_SHIELD_TICKS) / 60.0F);
                double targetY = this.groundY + (HOVER_HEIGHT * Math.sin(progress * Math.PI / 2.0));

                double diffY = targetY - this.getY();
                this.setDeltaMovement(0, diffY * 0.25D, 0);
                this.hasImpulse = true;
                this.fallDistance = 0.0F;

                applyNightForceField();
            } else {
                if (this.getAnimState() == STATE_SHIELD_NIGHT) {
                    this.entityData.set(ANIM_STATE, STATE_IDLE);
                    this.entityData.set(NIGHT_SHIELD_TICKS, 0);
                    this.groundY = Double.NaN;
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
                        this.idleFlavorCooldown = 240 + this.random.nextInt(200);
                        if (this.random.nextBoolean()) {
                            this.setAnimState(STATE_PONDER, 60);
                        } else {
                            this.setAnimState(STATE_CALIBRATE, 30);
                        }
                    }
                }
            }
        }
    }

                    private void applyNightForceField() {
        AABB searchBox = this.getBoundingBox().inflate(SHIELD_RADIUS, 3.5D, SHIELD_RADIUS);
        List<Entity> entities = this.level().getEntities(this, searchBox,
                e -> !(e instanceof Player) && !(e instanceof ItemEntity) && e.isAlive());

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