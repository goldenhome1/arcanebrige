package com.example.arcanebridge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "arcane_bridge", value = Dist.CLIENT)
public class ClientInputGlitchHandler {

    private static int glitchTicksRemaining = 0;

    public static void triggerGlitch(int ticks) {
        glitchTicksRemaining = ticks;
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal("§6§l⚙ [СБОЙ ПРИВОДОВ]: §cИнверсия управления WASD!"), true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && glitchTicksRemaining > 0) {
            glitchTicksRemaining--;
        }
    }

    @SubscribeEvent
    public static void onInputUpdate(MovementInputUpdateEvent event) {
        if (glitchTicksRemaining > 0) {
            Input input = event.getInput();
            // Инвертируем оси перемещения
            input.forwardImpulse = -input.forwardImpulse;
            input.leftImpulse = -input.leftImpulse;

            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.tickCount % 6 == 0) {
                input.jumping = false;
            }
        }
    }
}