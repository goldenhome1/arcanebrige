package com.example.arcanebridge.network;

import com.example.arcanebridge.client.render.ShieldBarOverlayRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundSyncShieldPacket {

    private final int entityId;
    private final CompoundTag shieldData;

    public ClientboundSyncShieldPacket(int entityId, CompoundTag shieldData) {
        this.entityId = entityId;
        this.shieldData = shieldData;
    }

    public static void encode(ClientboundSyncShieldPacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.entityId);
        buffer.writeNbt(msg.shieldData);
    }

    public static ClientboundSyncShieldPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundSyncShieldPacket(buffer.readInt(), buffer.readNbt());
    }

    public static void handle(ClientboundSyncShieldPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT, () -> () -> ShieldBarOverlayRenderer.handleSyncShieldPacket(msg.entityId, msg.shieldData)
        ));
        ctx.get().setPacketHandled(true);
    }
}