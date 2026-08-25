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

    private float lastObservedSpeed = Float.NaN;
    private float lastObservedCapacity = Float.NaN;

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
        this.lastObservedSpeed = Float.NaN;
        this.lastObservedCapacity = Float.NaN;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            if (isReceiver) {
                updateGeneratedRotation();
            }
        }
    }

    public void registerInNetwork() {
        this.isLinked = true;
        this.lastObservedSpeed = Float.NaN;
        this.lastObservedCapacity = Float.NaN;
        setChanged();
        if (level != null && !level.isClientSide && isReceiver) {
            updateGeneratedRotation();
        }
    }

    public void unregisterFromNetwork() {
        this.isLinked = false;
        setChanged();
        if (level != null && !level.isClientSide && isReceiver) {
            updateGeneratedRotation();
        }
    }

    @Override
    public boolean isSource() {
        // Источником вращения в Create выступает ИСКЛЮЧИТЕЛЬНО Приемник (RX)
        return isReceiver && isLinked && getGeneratedSpeed() != 0.0F;
    }

    @Override
    public float getGeneratedSpeed() {
        if (isReceiver && isLinked && level != null) {
            return PhaseNetworkManager.getChannelSpeed(level, channelId);
        }
        return 0.0F;
    }

    @Override
    public float calculateAddedStressCapacity() {
        if (isReceiver && isLinked && level != null) {
            float speed = Math.abs(getGeneratedSpeed());
            if (speed > 0.0F) {
                float totalCap = PhaseNetworkManager.getChannelCapacity(level, channelId);
                float effectiveCap = totalCap > 0.0F ? totalCap : 2048.0F;
                
                // Переводим общую мощность сети (SU) в удельную мощность на 1 RPM (SU/RPM)
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
                // ПЕРЕДАТЧИК (TX): пассивно считывает реальную скорость и мощность линии
                float currentSpeed = getSpeed();
                float currentCapacity = 0.0F;

                if (getOrCreateNetwork() != null) {
                    currentCapacity = getOrCreateNetwork().calculateCapacity();
                }

                if (Math.abs(currentSpeed - lastObservedSpeed) > 0.01F || Math.abs(currentCapacity - lastObservedCapacity) > 0.1F) {
                    lastObservedSpeed = currentSpeed;
                    lastObservedCapacity = currentCapacity;
                    PhaseNetworkManager.updateChannel(channelId, currentSpeed, currentCapacity);
                }
            } else {
                // ПРИЕМНИК (RX): слушает изменения частоты в эфире и мгновенно пересчитывает сеть Create
                float targetSpeed = PhaseNetworkManager.getChannelSpeed(level, channelId);
                float targetCapacity = PhaseNetworkManager.getChannelCapacity(level, channelId);

                if (Math.abs(targetSpeed - lastObservedSpeed) > 0.01F || Math.abs(targetCapacity - lastObservedCapacity) > 0.1F) {
                    lastObservedSpeed = targetSpeed;
                    lastObservedCapacity = targetCapacity;
                    updateGeneratedRotation();
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
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        this.isReceiver = compound.getBoolean("IsReceiver");
        this.channelId = compound.getDouble("ChannelId");
        this.isLinked = compound.getBoolean("IsLinked");
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