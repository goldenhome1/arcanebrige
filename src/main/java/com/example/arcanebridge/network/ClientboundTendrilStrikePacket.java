package com.example.arcanebridge.network;

import com.example.arcanebridge.client.ResonanceVfxHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundTendrilStrikePacket {
    private final double startX, startY, startZ;
    private final double endX, endY, endZ;
    private final int stacks;
    private final boolean isElemental;

    public ClientboundTendrilStrikePacket(double startX, double startY, double startZ,
                                         double endX, double endY, double endZ,
                                         int stacks, boolean isElemental) {
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.endX = endX;
        this.endY = endY;
        this.endZ = endZ;
        this.stacks = stacks;
        this.isElemental = isElemental;
    }

    public ClientboundTendrilStrikePacket(FriendlyByteBuf buf) {
        this.startX = buf.readDouble();
        this.startY = buf.readDouble();
        this.startZ = buf.readDouble();
        this.endX = buf.readDouble();
        this.endY = buf.readDouble();
        this.endZ = buf.readDouble();
        this.stacks = buf.readInt();
        this.isElemental = buf.readBoolean();
    }

    public static void encode(ClientboundTendrilStrikePacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.startX);
        buf.writeDouble(msg.startY);
        buf.writeDouble(msg.startZ);
        buf.writeDouble(msg.endX);
        buf.writeDouble(msg.endY);
        buf.writeDouble(msg.endZ);
        buf.writeInt(msg.stacks);
        buf.writeBoolean(msg.isElemental);
    }

    public static ClientboundTendrilStrikePacket decode(FriendlyByteBuf buf) {
        return new ClientboundTendrilStrikePacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ResonanceVfxHandler.handleTendrilStrikePacket(
                this.startX, this.startY, this.startZ, this.endX, this.endY, this.endZ, this.stacks, this.isElemental)));
        context.setPacketHandled(true);
    }
}
