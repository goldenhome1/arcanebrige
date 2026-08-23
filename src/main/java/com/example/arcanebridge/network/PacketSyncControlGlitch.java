package com.example.arcanebridge.network;

import com.example.arcanebridge.client.ClientInputGlitchHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncControlGlitch {
    private final int glitchTicks;

    public PacketSyncControlGlitch(int glitchTicks) {
        this.glitchTicks = glitchTicks;
    }

    public PacketSyncControlGlitch(FriendlyByteBuf buf) {
        this.glitchTicks = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.glitchTicks);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientInputGlitchHandler.triggerGlitch(glitchTicks));
        });
        ctx.get().setPacketHandled(true);
    }
}