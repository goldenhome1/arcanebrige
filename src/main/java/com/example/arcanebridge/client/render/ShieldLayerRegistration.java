package com.example.arcanebridge.client.render;

import com.example.arcanebridge.combat.MobArchetypes;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MobArchetypes.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ShieldLayerRegistration {

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // Подключаем скафандр ко всем зарегистрированным сущностям
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            try {
                LivingEntityRenderer renderer = event.getRenderer((EntityType) type);
                if (renderer != null) {
                    renderer.addLayer(new ShieldSuitLayer(renderer));
                }
            } catch (Throwable ignored) {}
        }

        // Подключаем к скинам игроков на случай дуэлей / PVP
        for (String skin : event.getSkins()) {
            try {
                LivingEntityRenderer playerRenderer = event.getSkin(skin);
                if (playerRenderer != null) {
                    playerRenderer.addLayer(new ShieldSuitLayer(playerRenderer));
                }
            } catch (Throwable ignored) {}
        }
    }
}