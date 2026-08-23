package com.example.arcanebridge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public class KeyInputHandler {

    public static final KeyMapping HUD_MENU_KEY = new KeyMapping(
            "key.arcane_bridge.hud_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H, // Кнопка H по умолчанию
            "key.categories.arcane_bridge"
    );

    @Mod.EventBusSubscriber(modid = "arcane_bridge", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(HUD_MENU_KEY);
        }
    }

    @Mod.EventBusSubscriber(modid = "arcane_bridge", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && HUD_MENU_KEY.consumeClick()) {
                if (mc.screen == null) {
                    mc.setScreen(new HudSettingsScreen());
                }
            }
        }
    }
}