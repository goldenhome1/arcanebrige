package com.example.arcanebridge.hex.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.registries.Registries;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PhaseNetworkManager extends SavedData {

    private static final String DATA_NAME = "arcane_bridge_phase_network";

    public record ChannelEndpoint(ResourceKey<Level> dimension, BlockPos pos) {}

    public static class PhaseChannel {
        public ChannelEndpoint transmitter;
        public ChannelEndpoint receiver;
        public float currentSpeed = 0.0f;
    }

    private final Map<Double, PhaseChannel> channels = new ConcurrentHashMap<>();

    public static PhaseNetworkManager get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return new PhaseNetworkManager();
        return overworld.getDataStorage().computeIfAbsent(PhaseNetworkManager::load, PhaseNetworkManager::new, DATA_NAME);
    }

    public void registerTransmitter(double channelId, ResourceKey<Level> dim, BlockPos pos) {
        PhaseChannel channel = channels.computeIfAbsent(channelId, k -> new PhaseChannel());
        channel.transmitter = new ChannelEndpoint(dim, pos);
        setDirty();
    }

    public void registerReceiver(double channelId, ResourceKey<Level> dim, BlockPos pos) {
        PhaseChannel channel = channels.computeIfAbsent(channelId, k -> new PhaseChannel());
        channel.receiver = new ChannelEndpoint(dim, pos);
        setDirty();
    }

    public PhaseChannel getChannel(double channelId) {
        return channels.get(channelId);
    }

    public static PhaseNetworkManager load(CompoundTag tag) {
        PhaseNetworkManager manager = new PhaseNetworkManager();
        ListTag list = tag.getList("Channels", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag cTag = list.getCompound(i);
            double id = cTag.getDouble("ChannelId");
            PhaseChannel channel = new PhaseChannel();

            if (cTag.contains("TxDim")) {
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(cTag.getString("TxDim")));
                BlockPos pos = BlockPos.of(cTag.getLong("TxPos"));
                channel.transmitter = new ChannelEndpoint(dim, pos);
            }
            if (cTag.contains("RxDim")) {
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(cTag.getString("RxDim")));
                BlockPos pos = BlockPos.of(cTag.getLong("RxPos"));
                channel.receiver = new ChannelEndpoint(dim, pos);
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
            cTag.putDouble("ChannelId", entry.getKey());
            if (entry.getValue().transmitter != null) {
                cTag.putString("TxDim", entry.getValue().transmitter.dimension().location().toString());
                cTag.putLong("TxPos", entry.getValue().transmitter.pos().asLong());
            }
            if (entry.getValue().receiver != null) {
                cTag.putString("RxDim", entry.getValue().receiver.dimension().location().toString());
                cTag.putLong("RxPos", entry.getValue().receiver.pos().asLong());
            }
            list.add(cTag);
        }
        tag.put("Channels", list);
        return tag;
    }
}