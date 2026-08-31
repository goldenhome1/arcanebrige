package com.example.arcanebridge.client.render;

import com.example.arcanebridge.combat.MobArchetypes;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.event.GeoRenderEvent;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

import java.lang.reflect.Field;
import java.util.Map;

public class ShieldLayerRegistration {

    // 1. Регистрация на шине MOD: Ванильные сущности и прямой доступ к GeckoLib рендерерам
    @Mod.EventBusSubscriber(modid = MobArchetypes.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {

        @SubscribeEvent
        @SuppressWarnings({"unchecked", "rawtypes"})
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            try {
                // Извлекаем внутреннюю карту рендереров Forge напрямую в обход ClassCastException
                Field renderersField = EntityRenderersEvent.AddLayers.class.getDeclaredField("renderers");
                renderersField.setAccessible(true);
                Map<EntityType<?>, EntityRenderer<?>> renderers = (Map<EntityType<?>, EntityRenderer<?>>) renderersField.get(event);

                if (renderers != null) {
                    for (EntityRenderer<?> renderer : renderers.values()) {
                        if (renderer instanceof LivingEntityRenderer livingRenderer) {
                            livingRenderer.addLayer(new ShieldSuitLayer(livingRenderer));
                        } else if (renderer instanceof GeoEntityRenderer geoRenderer) {
                            geoRenderer.addRenderLayer(new GeoShieldSuitLayer(geoRenderer));
                        } else if (renderer instanceof GeoReplacedEntityRenderer geoReplacedRenderer) {
                            geoReplacedRenderer.addRenderLayer(new GeoShieldSuitLayer(geoReplacedRenderer));
                        }
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }

            // Подключение слоя к скинам игроков
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

    // 2. Регистрация на шине FORGE: Нативный перехват компиляции слоев GeckoLib 4
    @Mod.EventBusSubscriber(modid = MobArchetypes.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeBusEvents {

        @SubscribeEvent
        @SuppressWarnings({"unchecked", "rawtypes"})
        public static void onGeckoCompileRenderLayers(GeoRenderEvent.Entity.CompileRenderLayers event) {
            event.addLayer(new GeoShieldSuitLayer(event.getRenderer()));
        }
    }
}