package com.example.arcanebridge.network;

import com.example.arcanebridge.client.RiftSkyRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ClientboundRaidVfxPacket {

    private final boolean isPrepActive;
    private final int prepTimerSeconds;
    private final int maxPrepSeconds;
    private final boolean isRaidActive;
    private final String raidType;
    private final UUID targetPlayerUUID;
    private final BlockPos epicenter;

    public ClientboundRaidVfxPacket(boolean isPrepActive, int prepTimerSeconds, int maxPrepSeconds,
                                   boolean isRaidActive, String raidType, UUID targetPlayerUUID, BlockPos epicenter) {
        this.isPrepActive = isPrepActive;
        this.prepTimerSeconds = prepTimerSeconds;
        this.maxPrepSeconds = maxPrepSeconds;
        this.isRaidActive = isRaidActive;
        this.raidType = raidType != null ? raidType : "arcane_breach";
        this.targetPlayerUUID = targetPlayerUUID != null ? targetPlayerUUID : new UUID(0L, 0L);
        this.epicenter = epicenter != null ? epicenter : BlockPos.ZERO;
    }

    public ClientboundRaidVfxPacket(FriendlyByteBuf buf) {
        this.isPrepActive = buf.readBoolean();
        this.prepTimerSeconds = buf.readInt();
        this.maxPrepSeconds = buf.readInt();
        this.isRaidActive = buf.readBoolean();
        this.raidType = buf.readUtf(64);
        this.targetPlayerUUID = buf.readUUID();
        this.epicenter = buf.readBlockPos();
    }

    public static void encode(ClientboundRaidVfxPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.isPrepActive);
        buf.writeInt(msg.prepTimerSeconds);
        buf.writeInt(msg.maxPrepSeconds);
        buf.writeBoolean(msg.isRaidActive);
        buf.writeUtf(msg.raidType);
        buf.writeUUID(msg.targetPlayerUUID);
        buf.writeBlockPos(msg.epicenter);
    }

    public static ClientboundRaidVfxPacket decode(FriendlyByteBuf buf) {
        return new ClientboundRaidVfxPacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                RiftSkyRenderer.updateRaidState(
                        this.isPrepActive,
                        this.prepTimerSeconds,
                        this.maxPrepSeconds,
                        this.isRaidActive,
                        this.raidType,
                        this.targetPlayerUUID,
                        this.epicenter
                );
            });
        });
        context.setPacketHandled(true);
    }
}