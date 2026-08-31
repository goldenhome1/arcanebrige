package com.example.arcanebridge.client.render;

import com.example.arcanebridge.combat.MobArchetypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class GeoShieldSuitLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {

    public GeoShieldSuitLayer(GeoRenderer<T> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {

        if (!(animatable instanceof LivingEntity entity) || !entity.isAlive()) return;

        CompoundTag data = entity.getPersistentData();
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

        float r, g, b;
        switch (typeStr) {
            case "ARMORED" -> { r = 1.00F; g = 0.80F; b = 0.20F; }
            case "ETHEREAL" -> { r = 0.88F; g = 0.38F; b = 1.00F; }
            case "BIO" -> { r = 0.25F; g = 0.95F; b = 0.40F; }
            default -> { r = 0.20F; g = 0.85F; b = 1.00F; }
        }

        float time = entity.tickCount + partialTick;
        float pulse = 1.065F + (float) Math.sin(time * 0.08F) * 0.015F;

        float alpha = 0.30F + (float) Math.sin(time * 0.08F) * 0.05F;
        if (entity.hurtTime > 0) {
            alpha = Math.min(0.75F, alpha + 0.40F);
            pulse += 0.02F;
        }

        ResourceLocation texture = getTextureResource(animatable);
        RenderType suitRenderType = RenderType.entityTranslucent(texture);
        VertexConsumer suitBuffer = bufferSource.getBuffer(suitRenderType);

        double midY = entity.getBbHeight() * 0.5D;

        poseStack.pushPose();
        // Динамическое масштабирование вокруг центра высоты модели GeckoLib
        poseStack.translate(0.0D, midY, 0.0D);
        poseStack.scale(pulse, pulse, pulse);
        poseStack.translate(0.0D, -midY, 0.0D);

        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, suitRenderType, suitBuffer,
                partialTick, 15728880, OverlayTexture.NO_OVERLAY, r, g, b, alpha);[cite: 4]

        poseStack.popPose();
    }
}