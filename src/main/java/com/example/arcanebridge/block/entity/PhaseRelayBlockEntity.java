package com.example.arcanebridge.block.entity;

import com.example.arcanebridge.hex.network.PhaseNetworkManager;
import com.example.arcanebridge.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class PhaseRelayBlockEntity extends GeneratingKineticBlockEntity {

    public double channelId = 1.0D;
    public boolean isReceiver = false;

    public PhaseRelayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public PhaseRelayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHASE_RELAY.get(), pos, state);
    }

    /**
     * Блок признается генератором ТОЛЬКО если он в режиме RX и имеет ненулевую скорость.
     * В режиме TX он работает как обычный пассивный потребитель/вал.
     */
    @Override
    public boolean isSource() {
        return isReceiver && Math.abs(getGeneratedSpeed()) > 0.01f;
    }

    @Override
    public void initialize() {
        super.initialize();
        registerInNetwork();
    }

    @Override
    public void destroy() {
        super.destroy();
        unregisterFromNetwork();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        unregisterFromNetwork();
    }

    public void registerInNetwork() {
        if (this.level != null && !this.level.isClientSide && this.level.getServer() != null) {
            PhaseNetworkManager manager = PhaseNetworkManager.get(this.level.getServer());
            if (isReceiver) {
                manager.registerReceiver(channelId, this.level.dimension(), this.worldPosition);
            } else {
                manager.registerTransmitter(channelId, this.level.dimension(), this.worldPosition);
            }
        }
    }

    public void unregisterFromNetwork() {
        if (this.level != null && !this.level.isClientSide && this.level.getServer() != null) {
            PhaseNetworkManager manager = PhaseNetworkManager.get(this.level.getServer());
            var channel = manager.getChannel(channelId);
            if (channel != null) {
                if (isReceiver && channel.receiver != null && channel.receiver.pos.equals(this.worldPosition)) {
                    channel.receiver = null;
                } else if (!isReceiver && channel.transmitter != null && channel.transmitter.pos.equals(this.worldPosition)) {
                    channel.transmitter = null;
                }
            }
        }
    }

    @Override
    public float getGeneratedSpeed() {
        if (!isReceiver) return 0.0f;
        if (this.level != null && !this.level.isClientSide) {
            MinecraftServer server = this.level.getServer();
            if (server != null) {
                PhaseNetworkManager manager = PhaseNetworkManager.get(server);
                PhaseNetworkManager.PhaseChannel channel = manager.getChannel(channelId);
                if (channel != null) {
                    return channel.currentSpeed;
                }
            }
        }
        return 0.0f;
    }

    @Override
    public float calculateAddedStressCapacity() {
        if (!isReceiver) return 0.0f;
        if (this.level != null && !this.level.isClientSide) {
            MinecraftServer server = this.level.getServer();
            if (server != null) {
                PhaseNetworkManager manager = PhaseNetworkManager.get(server);
                PhaseNetworkManager.PhaseChannel channel = manager.getChannel(channelId);
                if (channel != null && channel.txCapacity > 0) {
                    return channel.txCapacity;
                }
            }
        }
        return 102400.0f;
    }

    @Override
    public float calculateStressApplied() {
        if (isReceiver) return 0.0f;
        if (this.level != null && !this.level.isClientSide) {
            MinecraftServer server = this.level.getServer();
            if (server != null) {
                PhaseNetworkManager manager = PhaseNetworkManager.get(server);
                PhaseNetworkManager.PhaseChannel channel = manager.getChannel(channelId);
                if (channel != null && channel.rxStress > 0.0f) {
                    return channel.rxStress;
                }
            }
        }
        return 0.0f;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition).inflate(1.0D);
    }

    @Override
    public void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putDouble("ChannelId", channelId);
        tag.putBoolean("IsReceiver", isReceiver);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (tag.contains("ChannelId")) channelId = tag.getDouble("ChannelId");
        if (tag.contains("IsReceiver")) isReceiver = tag.getBoolean("IsReceiver");
    }
}