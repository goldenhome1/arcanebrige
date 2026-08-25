package com.example.arcanebridge.block.entity;

import net.minecraft.world.level.Level;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PhaseNetworkManager {
    private static final Map<Double, Float> CHANNEL_SPEEDS = new ConcurrentHashMap<>();

    public static void updateChannelSpeed(Level level, double channel, float speed) {
        CHANNEL_SPEEDS.put(channel, speed);
    }

    public static float getChannelSpeed(Level level, double channel) {
        return CHANNEL_SPEEDS.getOrDefault(channel, 0.0F);
    }
}