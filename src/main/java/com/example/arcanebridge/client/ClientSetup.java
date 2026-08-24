package com.example.arcanebridge.client;

import com.example.arcanebridge.ArcaneBridge;
import com.example.arcanebridge.client.render.ArcaneGuideRenderer;
import com.example.arcanebridge.entity.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ArcaneBridge.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ARCANE_GUIDE.get(), ArcaneGuideRenderer::new);
    }
}