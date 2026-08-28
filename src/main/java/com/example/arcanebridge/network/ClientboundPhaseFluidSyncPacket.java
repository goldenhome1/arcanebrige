package com.example.arcanebridge.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ClientboundPhaseFluidSyncPacket {

    // Клиентский кэш для отображения в очках инженера и HUD
    public static final Map<Integer, FluidTank> CLIENT_CHANNELS = new HashMap<>();

    private final int channelId;
    private final CompoundTag tankTag;

    public ClientboundPhaseFluidSyncPacket(int channelId, CompoundTag tankTag) {
        this.channelId = channelId;
        this.tankTag = tankTag;
    }

    public ClientboundPhaseFluidSyncPacket(FriendlyByteBuf buf) {
        this.channelId = buf.readVarInt();
        this.tankTag = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.channelId);
        buf.writeNbt(this.tankTag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            FluidTank clientTank = CLIENT_CHANNELS.computeIfAbsent(this.channelId, id -> new FluidTank(16000));
            if (this.tankTag != null) {
                clientTank.readFromNBT(this.tankTag);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}