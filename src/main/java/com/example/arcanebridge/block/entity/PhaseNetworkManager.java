package com.example.arcanebridge.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PhaseNetworkManager {

    private static final Map<ResourceLocation, Map<Double, Set<BlockPos>>> NETWORKS = new ConcurrentHashMap<>();

    private static ResourceLocation getDimId(LevelAccessor level) {
        if (level instanceof Level l) {
            return l.dimension().location();
        }
        return new ResourceLocation("minecraft", "overworld");
    }

    public static void addToChannel(LevelAccessor level, double channel, BlockPos pos) {
        ResourceLocation dim = getDimId(level);
        NETWORKS.computeIfAbsent(dim, d -> new ConcurrentHashMap<>())
                .computeIfAbsent(channel, c -> ConcurrentHashMap.newKeySet())
                .add(pos.immutable());
    }

    public static void removeFromChannel(LevelAccessor level, double channel, BlockPos pos) {
        ResourceLocation dim = getDimId(level);
        Map<Double, Set<BlockPos>> dimMap = NETWORKS.get(dim);
        if (dimMap != null) {
            Set<BlockPos> set = dimMap.get(channel);
            if (set != null) {
                set.remove(pos);
                if (set.isEmpty()) {
                    dimMap.remove(channel);
                }
            }
        }
    }

    public static Set<BlockPos> getChannelNodes(LevelAccessor level, double channel) {
        ResourceLocation dim = getDimId(level);
        Map<Double, Set<BlockPos>> dimMap = NETWORKS.get(dim);
        if (dimMap != null) {
            Set<BlockPos> set = dimMap.get(channel);
            if (set != null) {
                return set;
            }
        }
        return Collections.emptySet();
    }
}