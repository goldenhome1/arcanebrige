package com.example.arcanebridge.fluid;

import com.example.arcanebridge.network.ClientboundPhaseFluidSyncPacket;
import com.example.arcanebridge.network.ModMessages;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class PhaseFluidTank extends FluidTank {

    public static final int DEFAULT_CAPACITY = 16000; // 16 вёдер буфера на канал
    private final int channelId;

    public PhaseFluidTank(int channelId) {
        super(DEFAULT_CAPACITY);
        this.channelId = channelId;
    }

    @Override
    protected void onContentsChanged() {
        PhaseFluidSavedData data = PhaseFluidSavedData.getInstance();
        if (data != null) {
            data.setDirty();
        }
        // Синхронизируем уровень жидкости со всеми клиентами для очков инженера
        ModMessages.sendToAllClients(new ClientboundPhaseFluidSyncPacket(this.channelId, this.writeToNBT(new net.minecraft.nbt.CompoundTag())));
    }

    public int getChannelId() {
        return channelId;
    }
}