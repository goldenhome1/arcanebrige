package com.example.arcanebridge.block.entity;

import net.minecraft.world.level.Level;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PhaseNetworkManager {

    public static class ChannelData {
        public float speed = 0.0F;
        public float capacity = 0.0F;
        public float stress = 0.0F;

        public ChannelData(float speed, float capacity, float stress) {
            this.speed = speed;
            this.capacity = capacity;
            this.stress = stress;
        }
    }

    private static final Map<Double, ChannelData> CHANNELS = new ConcurrentHashMap<>();

    public static void updateTx(double channel, float speed, float capacity) {
        CHANNELS.compute(channel, (k, v) -> v == null 
                ? new ChannelData(speed, capacity, 0.0F) 
                : new ChannelData(speed, capacity, v.stress));
    }

    public static void updateRxStress(double channel, float stress) {
        CHANNELS.compute(channel, (k, v) -> v == null 
                ? new ChannelData(0.0F, 0.0F, stress) 
                : new ChannelData(v.speed, v.capacity, stress));
    }

    public static float getChannelSpeed(Level level, double channel) {
        ChannelData data = CHANNELS.get(channel);
        return data != null ? data.speed : 0.0F;
    }

    public static float getChannelCapacity(Level level, double channel) {
        ChannelData data = CHANNELS.get(channel);
        return data != null ? data.capacity : 0.0F;
    }

    public static float getChannelStress(Level level, double channel) {
        ChannelData data = CHANNELS.get(channel);
        return data != null ? data.stress : 0.0F;
    }
}