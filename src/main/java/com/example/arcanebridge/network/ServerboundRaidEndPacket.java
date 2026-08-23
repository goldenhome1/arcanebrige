package com.example.arcanebridge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundRaidEndPacket {

    public ServerboundRaidEndPacket() {}

    public ServerboundRaidEndPacket(FriendlyByteBuf buf) {}

    public static void encode(ServerboundRaidEndPacket msg, FriendlyByteBuf buf) {}

    public static ServerboundRaidEndPacket decode(FriendlyByteBuf buf) {
        return new ServerboundRaidEndPacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.getServer() != null) {
                // Выполняем внутренний сброс рейда от консоли сервера
                player.getServer().getCommands().performPrefixedCommand(
                        player.getServer().createCommandSourceStack(),
                        "arcane_internal_reset_raid"
                );
            }
        });
        context.setPacketHandled(true);
    }
}