package com.example.arcanebridge.block.entity;

import com.example.arcanebridge.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PhaseRelayBlockEntity extends GeneratingKineticBlockEntity {

    public boolean isReceiver = false;
    public double channelId = 0.0D;
    public boolean isLinked = false;

    // Синхронизированные данные для очков (Goggles) и рендера на клиенте
    private float syncedSpeed = 0.0F;
    private float syncedCapacity = 0.0F;

    private float lastObservedSpeed = -999999.0F;
    private float lastObservedCapacity = -999999.0F;

    public PhaseRelayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public PhaseRelayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHASE_RELAY.get(), pos, state);
    }

    public void tuneChannel(double channel, boolean receiver) {
        this.channelId = channel;
        this.isReceiver = receiver;
        this.isLinked = true;
        this.lastObservedSpeed = -999999.0F;
        this.lastObservedCapacity = -999999.0F;
        setChanged();
        if (level != null && !level.isClientSide) {
            if (isReceiver) {
                applyNewGeneratedSpeed();
            }
            notifyUpdate();
        }
    }

    public void registerInNetwork() {
        this.isLinked = true;
        this.lastObservedSpeed = -999999.0F;
        this.lastObservedCapacity = -999999.0F;
        setChanged();
        if (level != null && !level.isClientSide) {
            if (isReceiver) {
                applyNewGeneratedSpeed();
            }
            notifyUpdate();
        }
    }

    public void unregisterFromNetwork() {
        this.isLinked = false;
        this.lastObservedSpeed = -999999.0F;
        this.lastObservedCapacity = -999999.0F;
        setChanged();
        if (level != null && !level.isClientSide) {
            if (isReceiver) {
                applyNewGeneratedSpeed();
            }
            notifyUpdate();
        }
    }

    @Override
    public boolean isSource() {
        return isReceiver && isLinked && getGeneratedSpeed() != 0.0F;
    }

    @Override
    public float getGeneratedSpeed() {
        if (isReceiver && isLinked) {
            if (level != null && !level.isClientSide) {
                return PhaseNetworkManager.getChannelSpeed(level, channelId);
            }
            return syncedSpeed;
        }
        return 0.0F;
    }

    @Override
    public float calculateAddedStressCapacity() {
        if (isReceiver && isLinked) {
            float speed = Math.abs(getGeneratedSpeed());
            if (speed > 0.0F) {
                float totalCap = (level != null && !level.isClientSide)
                        ? PhaseNetworkManager.getChannelCapacity(level, channelId)
                        : syncedCapacity;
                float effectiveCap = totalCap > 0.0F ? totalCap : 2048.0F;
                
                // В Create мощность задается на 1 RPM (SU/RPM), движок сам умножает её на скорость
                float capPerRpm = effectiveCap / speed;
                this.lastCapacityProvided = capPerRpm;
                return capPerRpm;
            }
        }
        this.lastCapacityProvided = 0.0F;
        return 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;

        if (isLinked) {
            if (!isReceiver) {
                // ПЕРЕДАТЧИК (TX): транслирует реальную скорость и мощность линии в эфир
                float currentSpeed = getSpeed();
                float currentCapacity = (getOrCreateNetwork() != null) ? getOrCreateNetwork().calculateCapacity() : 0.0F;

                if (Math.abs(currentSpeed - lastObservedSpeed) > 0.01F || Math.abs(currentCapacity - lastObservedCapacity) > 0.1F) {
                    lastObservedSpeed = currentSpeed;
                    lastObservedCapacity = currentCapacity;
                    PhaseNetworkManager.updateChannel(channelId, currentSpeed, currentCapacity);
                    notifyUpdate();
                }
            } else {
                // ПРИЕМНИК (RX): слушает эфир и мгновенно пересчитывает физику Create
                float targetSpeed = PhaseNetworkManager.getChannelSpeed(level, channelId);
                float targetCapacity = PhaseNetworkManager.getChannelCapacity(level, channelId);

                if (Math.abs(targetSpeed - lastObservedSpeed) > 0.01F || Math.abs(targetCapacity - lastObservedCapacity) > 0.1F) {
                    lastObservedSpeed = targetSpeed;
                    lastObservedCapacity = targetCapacity;
                    applyNewGeneratedSpeed();
                    notifyUpdate();
                }
            }
        }
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putBoolean("IsReceiver", isReceiver);
        compound.putDouble("ChannelId", channelId);
        compound.putBoolean("IsLinked", isLinked);

        float curSpeed = isReceiver
                ? (level != null ? PhaseNetworkManager.getChannelSpeed(level, channelId) : 0.0F)
                : getSpeed();
        float curCap = isReceiver
                ? (level != null ? PhaseNetworkManager.getChannelCapacity(level, channelId) : 0.0F)
                : ((getOrCreateNetwork() != null) ? getOrCreateNetwork().calculateCapacity() : 0.0F);

        compound.putFloat("SyncedSpeed", curSpeed);
        compound.putFloat("SyncedCapacity", curCap);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        this.isReceiver = compound.getBoolean("IsReceiver");
        this.channelId = compound.getDouble("ChannelId");
        this.isLinked = compound.getBoolean("IsLinked");
        this.syncedSpeed = compound.getFloat("SyncedSpeed");
        this.syncedCapacity = compound.getFloat("SyncedCapacity");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        write(tag, true);
        return tag;
    }
}