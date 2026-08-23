package com.example.arcanebridge.client;

import com.example.arcanebridge.logic.CyberwareHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = "arcane_bridge", value = Dist.CLIENT)
public class HealthImplantHandler {

    private static boolean lastActiveState = true;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.tickCount % 5 != 0) return;

        boolean hasImplant = hasTacticalImplant(player);
        boolean configEnabled = HudConfig.showHealthBars;

        // Итоговое состояние: полоска активна ТОЛЬКО если есть имплант И тумблер в меню включен
        boolean shouldBeActive = hasImplant && configEnabled;

        if (shouldBeActive != lastActiveState) {
            lastActiveState = shouldBeActive;
            toggleNeat(shouldBeActive);
        }
    }

    private static void toggleNeat(boolean active) {
        try {
            Class<?> neatConfig = Class.forName("vazkii.neat.NeatConfig");
            Field drawField = neatConfig.getField("draw");
            drawField.setAccessible(true);
            Object rawValue = drawField.get(null);

            if (rawValue instanceof ForgeConfigSpec.BooleanValue spec) {
                if (spec.get() != active) {
                    spec.set(active);
                }
            } else if (rawValue instanceof Boolean currentBool) {
                if (currentBool != active) {
                    drawField.setBoolean(null, active);
                }
            }
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        boolean hideHealth = !hasTacticalImplant(player) || !HudConfig.showHealthBars;
        String rendererName = event.getRenderer().getClass().getName().toLowerCase();

        if (hideHealth && rendererName.contains("neat")) {
            event.setCanceled(true);
        }
    }

    public static boolean hasTacticalImplant(Player player) {
        if (player == null) return false;
        return CyberwareHelper.isCyberwareHudActive(player);
    }
}