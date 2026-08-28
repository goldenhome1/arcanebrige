package com.example.arcanebridge.fluid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class PhaseFluidSavedData extends SavedData {

    private static final String DATA_NAME = "arcane_bridge_phase_fluids";
    private static PhaseFluidSavedData instance;

    private final Map<Integer, PhaseFluidTank> channels = new HashMap<>();

    public static PhaseFluidSavedData getInstance() {
        return instance;
    }

    public static void init(ServerLevel level) {
        instance = level.getServer().overworld().getDataStorage().computeIfAbsent(
                PhaseFluidSavedData::load,
                PhaseFluidSavedData::new,
                DATA_NAME
        );
    }

    public PhaseFluidTank getOrCreateChannel(int channelId) {
        return channels.computeIfAbsent(channelId, PhaseFluidTank::new);
    }

    public static PhaseFluidSavedData load(CompoundTag nbt) {
        PhaseFluidSavedData data = new PhaseFluidSavedData();
        ListTag list = nbt.getList("Channels", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag channelTag = list.getCompound(i);
            int id = channelTag.getInt("ChannelId");
            PhaseFluidTank tank = new PhaseFluidTank(id);
            tank.readFromNBT(channelTag.getCompound("Tank"));
            data.channels.put(id, tank);
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag) {
        ListTag list = new ListTag();
        for (Map.Entry<Integer, PhaseFluidTank> entry : channels.entrySet()) {
            CompoundTag channelTag = new CompoundTag();
            channelTag.putInt("ChannelId", entry.getKey());
            channelTag.put("Tank", entry.getValue().writeToNBT(new CompoundTag()));
            list.add(channelTag);
        }
        compoundTag.put("Channels", list);
        return compoundTag;
    }
}