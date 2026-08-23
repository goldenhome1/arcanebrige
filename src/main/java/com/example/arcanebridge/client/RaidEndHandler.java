package com.example.arcanebridge.client;

import com.example.arcanebridge.network.NetworkHandler;
import com.example.arcanebridge.network.ServerboundRaidEndPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "arcane_bridge")
public class RaidEndHandler {

    @SubscribeEvent
    public static void onChatReceived(ClientChatReceivedEvent event) {
        if (event.getMessage() != null) {
            String text = event.getMessage().getString().toLowerCase();

            // Проверяем сообщение окончания рейда от Ravents
            if (text.contains("рейд завершен") || text.contains("рейд завершён") || text.contains("raid completed")) {
                NetworkHandler.sendToServer(new ServerboundRaidEndPacket());
            }
        }
    }
}