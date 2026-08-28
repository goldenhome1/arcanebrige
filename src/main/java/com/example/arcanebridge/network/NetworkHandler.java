package com.example.arcanebridge.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("arcane_bridge", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        // 1. Синхронизация резонанса
        CHANNEL.messageBuilder(ClientboundResonanceSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundResonanceSyncPacket::encode)
                .decoder(ClientboundResonanceSyncPacket::decode)
                .consumerMainThread(ClientboundResonanceSyncPacket::handle)
                .add();

        // 2. Сбой управления WASD
        CHANNEL.messageBuilder(PacketSyncControlGlitch.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketSyncControlGlitch::toBytes)
                .decoder(PacketSyncControlGlitch::new)
                .consumerMainThread(PacketSyncControlGlitch::handle)
                .add();

        // 3. Визуальные эффекты рейдов
        CHANNEL.messageBuilder(ClientboundRaidVfxPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundRaidVfxPacket::encode)
                .decoder(ClientboundRaidVfxPacket::decode)
                .consumerMainThread(ClientboundRaidVfxPacket::handle)
                .add();

        // 4. Завершение рейда
        CHANNEL.messageBuilder(ServerboundRaidEndPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerboundRaidEndPacket::encode)
                .decoder(ServerboundRaidEndPacket::decode)
                .consumerMainThread(ServerboundRaidEndPacket::handle)
                .add();

                // 5. Завершение калибровки брони (GUI -> Сервер)
        CHANNEL.messageBuilder(ServerboundRepairCompletePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerboundRepairCompletePacket::encode)
                .decoder(ServerboundRepairCompletePacket::decode)
                .consumerMainThread(ServerboundRepairCompletePacket::handle)
                .add();

                // 6. Запрос действий Гида / Локатора (GUI -> Сервер)
        CHANNEL.messageBuilder(ServerboundGuideActionPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerboundGuideActionPacket::toBytes)
                .decoder(ServerboundGuideActionPacket::new)
                .consumerMainThread(ServerboundGuideActionPacket::handle)
                .add();

        // 7. Синхронизация фазовых резервуаров жидкости (Сервер -> Клиент)
        CHANNEL.messageBuilder(ClientboundPhaseFluidSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundPhaseFluidSyncPacket::toBytes)
                .decoder(ClientboundPhaseFluidSyncPacket::new)
                .consumerMainThread(ClientboundPhaseFluidSyncPacket::handle)
                .add();
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        if (player != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
        }
    }

    public static <MSG> void sendToPlayer(ServerPlayer player, MSG message) {
        if (player != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
        }
    }

    public static <MSG> void sendToServer(MSG message) {
        CHANNEL.sendToServer(message);
    }

    public static <MSG> void sendToAll(MSG message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }
}