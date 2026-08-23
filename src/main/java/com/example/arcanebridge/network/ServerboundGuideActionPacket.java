package com.example.arcanebridge.network;

import com.example.arcanebridge.entity.ArcaneGuideEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundGuideActionPacket {
    private final int entityId;
    private final String actionType;
    private final String targetId;

    public ServerboundGuideActionPacket(int entityId, String actionType, String targetId) {
        this.entityId = entityId;
        this.actionType = actionType;
        this.targetId = targetId;
    }

    public ServerboundGuideActionPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.actionType = buf.readUtf();
        this.targetId = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeUtf(this.actionType);
        buf.writeUtf(this.targetId);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.level() != null) {
                Entity entity = player.level().getEntity(this.entityId);
                if (entity instanceof ArcaneGuideEntity guide) {
                    if ("LOCATE".equals(this.actionType)) {
                        guide.performStructureScan(player, this.targetId);
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}