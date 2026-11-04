package com.example.arcanebridge.entity;

import com.example.arcanebridge.data.ConsortiumSavedData;
import com.example.arcanebridge.entity.merchant.ConsortiumTradeGenerator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ConsortiumMerchantEntity extends Villager {

    private long lastTradeDay = -1;

    public ConsortiumMerchantEntity(EntityType<? extends ConsortiumMerchantEntity> entityType, Level level) {
        super(entityType, level);
        this.setInvulnerable(true);
        this.setPersistenceRequired();

        // Обычный аккуратный вид: житель равнин с профессией картографа (стильный монокль)
        this.setVillagerData(this.getVillagerData()
                .setType(VillagerType.PLAINS)
                .setProfession(VillagerProfession.CARTOGRAPHER)
                .setLevel(5));

        // Устанавливаем имя для окна торговли, но скрываем светящуюся плашку над головой
        this.setCustomName(Component.translatable("entity.arcane_bridge.consortium_merchant"));
        this.setCustomNameVisible(false);

        // Обнуляем скорость передвижения, чтобы стоял на месте
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        // Переопределяем пустым:
        // 1. Отключает ванильный Brain stroll (житель не ходит).
        // 2. Предотвращает вызов stopTrading(), из-за которого сразу закрывалось меню.
    }

    @Override
    public void travel(Vec3 travelVector) {
        // Запрещаем горизонтальное перемещение, но сохраняем гравитацию (падение, если под ним сломать блок)
        super.travel(new Vec3(0, travelVector.y, 0));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        // Если зажат Shift — пропускаем действие, чтобы моды вроде Carry On могли взять жителя на руки
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (this.isAlive() && !this.isTrading()) {
            if (this.getOffers().isEmpty()) {
                refreshDailyTrades();
            }

            this.setTradingPlayer(player);
            this.openTradingScreen(player, this.getDisplayName(), this.getVillagerData().getLevel());
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            long currentDay = this.level().getDayTime() / 24000L;

            if (currentDay != this.lastTradeDay) {
                this.lastTradeDay = currentDay;
                this.refreshDailyTrades();
            }

            // Периодически обновляем координаты в глобальном сохранении мира
            if (this.tickCount % 100 == 0 && this.level() instanceof ServerLevel serverLevel) {
                ConsortiumSavedData data = ConsortiumSavedData.get(serverLevel);
                if (this.getUUID().equals(data.getMerchantUUID())) {
                    data.updatePosition(serverLevel.dimension(), this.blockPosition());
                }
            }
        }
    }

    public void refreshDailyTrades() {
        MerchantOffers newOffers = ConsortiumTradeGenerator.generateDailyOffers(this, this.getRandom());
        this.offers = newOffers;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && player.isCreative()) {
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && !this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            ConsortiumSavedData data = ConsortiumSavedData.get(serverLevel);
            if (this.getUUID().equals(data.getMerchantUUID())) {
                data.clearMerchant();
            }
        }
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("LastTradeDay", this.lastTradeDay);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("LastTradeDay")) {
            this.lastTradeDay = tag.getLong("LastTradeDay");
        }
    }
}
