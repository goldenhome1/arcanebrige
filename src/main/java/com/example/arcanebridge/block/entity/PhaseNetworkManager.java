package com.example.arcanebridge.block.entity;

import net.minecraft.world.level.Level;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PhaseNetworkManager {

    public static class ChannelData {
        public float speed = 0.0F;
        public float capacity = 0.0F;

        public ChannelData(float speed, float capacity) {
            this.speed = speed;
            this.capacity = capacity;
        }
    }

    private static final Map<Double, ChannelData> CHANNELS = new ConcurrentHashMap<>();

    public static void updateChannel(double channel, float speed, float capacity) {
        CHANNELS.put(channel, new ChannelData(speed, capacity));
    }

    public static float getChannelSpeed(Level level, double channel) {
        ChannelData data = CHANNELS.get(channel);
        return data != null ? data.speed : 0.0F;
    }

    public static float getChannelCapacity(Level level, double channel) {
        ChannelData data = CHANNELS.get(channel);
        return data != null ? data.capacity : 0.0F;
    }
}