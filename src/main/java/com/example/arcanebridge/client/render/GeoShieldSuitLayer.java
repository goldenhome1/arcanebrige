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

    private static final ResourceLocation SHIELD_RUNES_TEX =
            new ResourceLocation("arcane_bridge", "textures/vfx/shield_runes.png");

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
            case "ARMORED" -> {
                r = 1.00F; g = 0.75F; b = 0.15F; // Латунь / Золото
            }
            case "ETHEREAL" -> {
                r = 0.85F; g = 0.30F; b = 1.00F; // Неоновый Аметист
            }
            case "BIO" -> {
                r = 0.20F; g = 1.00F; b = 0.40F; // Токсичный Био-зеленый
            }
            default -> {
                r = 0.20F; g = 0.85F; b = 1.00F;
            }
        }

        float time = entity.tickCount + partialTick;

        // Плавное смещение координат для живой анимации течения маны
        float uOffset = (time * 0.006F) % 1.0F;
        float vOffset = (time * 0.010F) % 1.0F;

        RenderType energySwirlType = RenderType.energySwirl(SHIELD_RUNES_TEX, uOffset, vOffset);
        VertexConsumer suitBuffer = bufferSource.getBuffer(energySwirlType);

        float pulse = 1.035F + (float) Math.sin(time * 0.08F) * 0.010F;
        float alpha = 0.85F;
        if (entity.hurtTime > 0) {
            pulse += 0.025F;
            r = Math.min(1.0F, r + 0.4F);
            g = Math.min(1.0F, g + 0.4F);
            b = Math.min(1.0F, b + 0.4F);
        }

        double midY = entity.getBbHeight() * 0.5D;

        poseStack.pushPose();
        poseStack.translate(0.0D, midY, 0.0D);
        poseStack.scale(pulse, pulse, pulse);
        poseStack.translate(0.0D, -midY, 0.0D);

        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, energySwirlType, suitBuffer,
                partialTick, 15728880, OverlayTexture.NO_OVERLAY, r, g, b, alpha);

        poseStack.popPose();
    }
}