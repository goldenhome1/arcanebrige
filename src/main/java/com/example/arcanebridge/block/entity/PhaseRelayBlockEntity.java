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
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            updateGeneratedRotation();
        }
    }

    public void registerInNetwork() {
        this.isLinked = true;
        setChanged();
        if (level != null && !level.isClientSide) {
            updateGeneratedRotation();
        }
    }

    public void unregisterFromNetwork() {
        this.isLinked = false;
        setChanged();
        if (level != null && !level.isClientSide) {
            updateGeneratedRotation();
        }
    }

    @Override
    public void updateGeneratedRotation() {
        super.updateGeneratedRotation();
    }

    @Override
    public float getGeneratedSpeed() {
        if (isReceiver && isLinked && level != null) {
            return PhaseNetworkManager.getChannelSpeed(level, channelId);
        }
        return 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide) {
            if (!isReceiver && isLinked) {
                PhaseNetworkManager.updateChannelSpeed(level, channelId, getSpeed());
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