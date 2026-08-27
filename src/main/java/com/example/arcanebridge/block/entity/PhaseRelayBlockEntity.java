package com.example.arcanebridge.block.entity;

import com.example.arcanebridge.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Set;

public class PhaseRelayBlockEntity extends KineticBlockEntity {

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
        if (this.isLinked && this.level != null && !this.level.isClientSide) {
            PhaseNetworkManager.removeFromChannel(this.level, this.channelId, this.worldPosition);
            notifyChannelMembers(this.channelId);
        }

        this.channelId = channel;
        this.isReceiver = receiver;
        this.isLinked = true;
        setChanged();

        if (this.level != null && !this.level.isClientSide) {
            PhaseNetworkManager.addToChannel(this.level, this.channelId, this.worldPosition);
            detachKinetics();
            attachKinetics();
            notifyChannelMembers(this.channelId);
            notifyUpdate();
        }
    }

    public void unregisterFromNetwork() {
        if (this.isLinked && this.level != null && !this.level.isClientSide) {
            PhaseNetworkManager.removeFromChannel(this.level, this.channelId, this.worldPosition);
            notifyChannelMembers(this.channelId);
        }
        this.isLinked = false;
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            detachKinetics();
            removeSource();
            notifyUpdate();
        }
    }

    @Override
    public List<BlockPos> addPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours) {
        super.addPropagationLocations(block, state, neighbours);
        if (this.isLinked && this.level != null) {
            Set<BlockPos> nodes = PhaseNetworkManager.getChannelNodes(this.level, this.channelId);
            for (BlockPos targetPos : nodes) {
                if (!targetPos.equals(this.worldPosition)) {
                    neighbours.add(targetPos);
                }
            }
        }
        return neighbours;
    }

    @Override
    public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo,
                                     BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs) {
        if (target instanceof PhaseRelayBlockEntity otherRelay) {
            if (this.isLinked && otherRelay.isLinked && this.channelId == otherRelay.channelId) {
                return 1.0F;
            }
        }
        return super.propagateRotationTo(target, stateFrom, stateTo, diff, connectedViaAxes, connectedViaCogs);
    }

    @Override
    public boolean isNoisy() {
        return false;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (this.isLinked && this.level != null && !this.level.isClientSide) {
            PhaseNetworkManager.addToChannel(this.level, this.channelId, this.worldPosition);
            detachKinetics();
            attachKinetics();
            notifyChannelMembers(this.channelId);
        }
    }

    @Override
    public void setRemoved() {
        if (this.isLinked && this.level != null && !this.level.isClientSide) {
            PhaseNetworkManager.removeFromChannel(this.level, this.channelId, this.worldPosition);
            notifyChannelMembers(this.channelId);
            detachKinetics();
            removeSource();
        }
        super.setRemoved();
    }

    private void notifyChannelMembers(double channel) {
        if (this.level == null || this.level.isClientSide) return;
        Set<BlockPos> nodes = PhaseNetworkManager.getChannelNodes(this.level, channel);
        for (BlockPos pos : nodes) {
            if (!pos.equals(this.worldPosition) && this.level.isLoaded(pos)) {
                if (this.level.getBlockEntity(pos) instanceof PhaseRelayBlockEntity other) {
                    other.detachKinetics();
                    other.attachKinetics();
                }
            }
        }
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putBoolean("IsReceiver", this.isReceiver);
        compound.putDouble("ChannelId", this.channelId);
        compound.putBoolean("IsLinked", this.isLinked);
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