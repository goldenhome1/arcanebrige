package com.example.arcanebridge.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

public class ConsortiumSavedData extends SavedData {
    private static final String DATA_NAME = "arcane_consortium_merchant";
    private UUID merchantUUID = null;
    private ResourceKey<Level> dimension = Level.OVERWORLD;
    private BlockPos pos = BlockPos.ZERO;

    public static ConsortiumSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                ConsortiumSavedData::load,
                ConsortiumSavedData::new,
                DATA_NAME
        );
    }

    public static ConsortiumSavedData load(CompoundTag tag) {
        ConsortiumSavedData data = new ConsortiumSavedData();
        if (tag.hasUUID("MerchantUUID")) {
            data.merchantUUID = tag.getUUID("MerchantUUID");
            if (tag.contains("Dim")) {
                data.dimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(tag.getString("Dim")));
            }
            if (tag.contains("X") && tag.contains("Y") && tag.contains("Z")) {
                data.pos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (merchantUUID != null) {
            tag.putUUID("MerchantUUID", merchantUUID);
            tag.putString("Dim", dimension.location().toString());
            tag.putInt("X", pos.getX());
            tag.putInt("Y", pos.getY());
            tag.putInt("Z", pos.getZ());
        }
        return tag;
    }

    public boolean isMerchantActive(ServerLevel currentLevel) {
        if (merchantUUID == null) return false;
        ServerLevel targetLevel = currentLevel.getServer().getLevel(dimension);
        if (targetLevel == null) return false;

        Entity entity = targetLevel.getEntity(merchantUUID);
        if (entity != null) {
            if (!entity.isAlive()) {
                clearMerchant();
                return false;
            }
            return true;
        }

        // Если чанк не загружен, но UUID зарегистрирован в мире
        return true;
    }

    public void registerMerchant(UUID uuid, ResourceKey<Level> dim, BlockPos p) {
        this.merchantUUID = uuid;
        this.dimension = dim;
        this.pos = p;
        setDirty();
    }

    public void updatePosition(ResourceKey<Level> dim, BlockPos p) {
        this.dimension = dim;
        this.pos = p;
        setDirty();
    }

    public void clearMerchant() {
        this.merchantUUID = null;
        this.pos = BlockPos.ZERO;
        setDirty();
    }

    public BlockPos getPos() {
        return pos;
    }

    public UUID getMerchantUUID() {
        return merchantUUID;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }
}
