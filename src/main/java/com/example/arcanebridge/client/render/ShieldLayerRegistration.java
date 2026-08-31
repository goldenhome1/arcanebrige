package com.example.arcanebridge.client.render;

import com.example.arcanebridge.combat.MobArchetypes;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

@Mod.EventBusSubscriber(modid = MobArchetypes.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ShieldLayerRegistration {

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // Регистрация слоев для всех зарегистрированных сущностей мира
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            try {
                EntityRenderer<?> renderer = event.getRenderer((EntityType) type);
                if (renderer instanceof LivingEntityRenderer livingRenderer) {
                    livingRenderer.addLayer(new ShieldSuitLayer(livingRenderer));
                } else if (renderer instanceof GeoEntityRenderer geoRenderer) {
                    geoRenderer.addRenderLayer(new GeoShieldSuitLayer(geoRenderer));
                } else if (renderer instanceof GeoReplacedEntityRenderer geoReplacedRenderer) {
                    geoReplacedRenderer.addRenderLayer(new GeoShieldSuitLayer(geoReplacedRenderer));
                }
            } catch (Throwable ignored) {}
        }

        // Регистрация слоя для скинов игроков (на случай PvP)
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