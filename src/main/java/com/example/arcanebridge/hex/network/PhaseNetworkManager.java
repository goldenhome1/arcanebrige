package com.example.arcanebridge.hex.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class PhaseNetworkManager extends SavedData {

    public static class PhaseNode {
        public final ResourceKey<Level> dimension;
        public final BlockPos pos;

        public PhaseNode(ResourceKey<Level> dimension, BlockPos pos) {
            this.dimension = dimension;
            this.pos = pos;
        }
    }

    public static class PhaseChannel {
        public PhaseNode transmitter;
        public PhaseNode receiver;
        public float currentSpeed = 0.0f;
        public float rxStress = 0.0f;
        public float txCapacity = 0.0f;
    }

    private final Map<Double, PhaseChannel> channels = new HashMap<>();

    public static PhaseNetworkManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                PhaseNetworkManager::load,
                PhaseNetworkManager::new,
                "arcane_phase_network"
        );
    }

    public void registerTransmitter(double channelId, ResourceKey<Level> dim, BlockPos pos) {
        PhaseChannel channel = channels.computeIfAbsent(channelId, k -> new PhaseChannel());
        channel.transmitter = new PhaseNode(dim, pos);
        setDirty();
    }

    public void registerReceiver(double channelId, ResourceKey<Level> dim, BlockPos pos) {
        PhaseChannel channel = channels.computeIfAbsent(channelId, k -> new PhaseChannel());
        channel.receiver = new PhaseNode(dim, pos);
        setDirty();
    }

        public PhaseChannel getChannel(double channelId) {
        return channels.get(channelId);
    }

    public java.util.Collection<PhaseChannel> getAllChannels() {
        return channels.values();
    }

    public PhaseChannel getChannelByReceiver(ResourceKey<Level> dim, BlockPos pos) {
        for (PhaseChannel channel : channels.values()) {
            if (channel.receiver != null && channel.receiver.dimension.equals(dim) && channel.receiver.pos.equals(pos)) {
                return channel;
            }
        }
        return null;
    }

    public PhaseChannel getChannelByTransmitter(ResourceKey<Level> dim, BlockPos pos) {
        for (PhaseChannel channel : channels.values()) {
            if (channel.transmitter != null && channel.transmitter.dimension.equals(dim) && channel.transmitter.pos.equals(pos)) {
                return channel;
            }
        }
        return null;
    }

    public static PhaseNetworkManager load(CompoundTag tag) {
        PhaseNetworkManager manager = new PhaseNetworkManager();
        ListTag list = tag.getList("Channels", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag cTag = list.getCompound(i);
            double id = cTag.getDouble("Id");
            PhaseChannel channel = new PhaseChannel();
            if (cTag.contains("TxDim")) {
                channel.transmitter = new PhaseNode(
                        ResourceKey.create(Registries.DIMENSION, new ResourceLocation(cTag.getString("TxDim"))),
                        BlockPos.of(cTag.getLong("TxPos"))
                );
            }
            if (cTag.contains("RxDim")) {
                channel.receiver = new PhaseNode(
                        ResourceKey.create(Registries.DIMENSION, new ResourceLocation(cTag.getString("RxDim"))),
                        BlockPos.of(cTag.getLong("RxPos"))
                );
            }
            manager.channels.put(id, channel);
        }
        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<Double, PhaseChannel> entry : channels.entrySet()) {
            CompoundTag cTag = new CompoundTag();
            cTag.putDouble("Id", entry.getKey());
            if (entry.getValue().transmitter != null) {
                cTag.putString("TxDim", entry.getValue().transmitter.dimension.location().toString());
                cTag.putLong("TxPos", entry.getValue().transmitter.pos.asLong());
            }
            if (entry.getValue().receiver != null) {
                cTag.putString("RxDim", entry.getValue().receiver.dimension.location().toString());
                cTag.putLong("RxPos", entry.getValue().receiver.pos.asLong());
            }
            list.add(cTag);
        }
        tag.put("Channels", list);
        return tag;
    }
}