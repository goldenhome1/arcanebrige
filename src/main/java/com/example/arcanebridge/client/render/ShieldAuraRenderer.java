package com.example.arcanebridge.client.render;

import com.example.arcanebridge.combat.MobArchetypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;

@Mod.EventBusSubscriber(modid = MobArchetypes.MODID, value = Dist.CLIENT)
public class ShieldAuraRenderer {

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity target = event.getEntity();
        if (target == null || target instanceof Player || !target.isAlive()) return;

        CompoundTag data = target.getPersistentData();
        if (data.getBoolean(MobArchetypes.NBT_ALL_SHIELDS_BROKEN)) return;
        if (!data.contains(MobArchetypes.NBT_SHIELD_LAYERS, Tag.TAG_LIST)) return;

        ListTag layers = data.getList(MobArchetypes.NBT_SHIELD_LAYERS, Tag.TAG_COMPOUND);
        int currentIndex = data.getInt(MobArchetypes.NBT_CURRENT_LAYER_INDEX);
        if (layers.isEmpty() || currentIndex >= layers.size()) return;

        CompoundTag activeLayer = layers.getCompound(currentIndex);
        float currentHp = activeLayer.getFloat("HP");
        float maxHp = activeLayer.getFloat("MaxHP");
        if (maxHp <= 0.0f || currentHp <= 0.0f) return;

        String typeStr = activeLayer.getString("Type");

        // Цветовая палитра полупрозрачного силового скафандра
        float r, g, b;
        switch (typeStr) {
            case "ARMORED" -> {
                r = 1.00F; g = 0.78F; b = 0.20F; // Золотисто-латунный оттенок
            }
            case "ETHEREAL" -> {
                r = 0.85F; g = 0.35F; b = 1.00F; // Аметистово-розовый оттенок
            }
            case "BIO" -> {
                r = 0.20F; g = 0.95F; b = 0.35F; // Био-зеленый оттенок
            }
            default -> {
                r = 0.25F; g = 0.80F; b = 1.00F; // Лазурный
            }
        }

        float partialTick = event.getPartialTick();
        float time = target.tickCount + partialTick;

        // Плавная пульсация объема (дыхание поля на 5-7%)
        float pulseScale = 1.055F + (float) Math.sin(time * 0.08F) * 0.015F;

        // Базовая прозрачность с яркой вспышкой при получении удара (Hit Reaction)
        float baseAlpha = 0.28F + (float) Math.sin(time * 0.08F) * 0.05F;
        if (target.hurtTime > 0) {
            baseAlpha = Math.min(0.70F, baseAlpha + 0.35F);
            pulseScale += 0.02F;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        EntityRenderer<?> rawRenderer = event.getRenderer();

        // ----------------------------------------------------
        // ВЕТВЛЕНИЕ 1: Ванильный конвейер (LivingEntityRenderer)
        // ----------------------------------------------------
        if (rawRenderer instanceof LivingEntityRenderer livingRenderer) {
            renderVanillaInflatedAura(livingRenderer, target, poseStack, bufferSource, r, g, b, baseAlpha, pulseScale);
            return;
        }

        // ----------------------------------------------------
        // ВЕТВЛЕНИЕ 2: Движок GeckoLib (GeoRenderer)
        // ----------------------------------------------------
        renderGeckoLibInflatedAura(rawRenderer, target, poseStack, bufferSource, r, g, b, baseAlpha, pulseScale, partialTick);
    }

    /**
     * Ветка 1: Рендер раздутого слепка для ванильных EntityModel
     */
    @SuppressWarnings("unchecked")
    private static void renderVanillaInflatedAura(LivingEntityRenderer livingRenderer,
                                                  LivingEntity target,
                                                  PoseStack poseStack,
                                                  MultiBufferSource bufferSource,
                                                  float r, float g, float b, float a,
                                                  float scale) {
        EntityModel<LivingEntity> model = livingRenderer.getModel();
        if (model == null) return;

        ResourceLocation texture = livingRenderer.getTextureLocation(target);
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));

        poseStack.pushPose();
        // Равномерно раздуваем модель относительно центра сущности
        poseStack.scale(scale, scale, scale);

        // Полнояркостный эмиссионный свет (15728880) для мягкого свечения шейдерами
        model.renderToBuffer(poseStack, buffer, 15728880, OverlayTexture.NO_OVERLAY, r, g, b, a);
        poseStack.popPose();
    }

    /**
     * Ветка 2: Рендер раздутого слепка для GeckoLib с защитой от сбоев версий
     */
    private static void renderGeckoLibInflatedAura(EntityRenderer<?> renderer,
                                                   LivingEntity target,
                                                   PoseStack poseStack,
                                                   MultiBufferSource bufferSource,
                                                   float r, float g, float b, float a,
                                                   float scale,
                                                   float partialTick) {
        try {
            Class<?> geoRendererClass = Class.forName("software.bernie.geckolib.renderer.GeoRenderer");
            if (!geoRendererClass.isInstance(renderer)) return;

            poseStack.pushPose();
            poseStack.scale(scale, scale, scale);

            // Попытка вызова нативного метода defaultRender(...) GeckoLib 4
            for (Method method : renderer.getClass().getMethods()) {
                if (method.getName().equals("defaultRender") && method.getParameterCount() >= 8) {
                    method.setAccessible(true);
                    ResourceLocation tex = renderer.getTextureLocation(null);
                    RenderType renderType = RenderType.entityTranslucent(tex);
                    VertexConsumer buffer = bufferSource.getBuffer(renderType);

                    // Передача параметров матрицы, буфера и RGBA-модуляции
                    if (method.getParameterCount() == 12) {
                        method.invoke(renderer, poseStack, target, bufferSource, renderType, buffer,
                                partialTick, 15728880, OverlayTexture.NO_OVERLAY, r, g, b, a);
                    }
                    break;
                }
            }
            poseStack.popPose();
        } catch (Throwable ignored) {
            // Безопасный пропуск, если структура конкретного GeckoLib-моба блокирует перерисовку
        }
    }
}