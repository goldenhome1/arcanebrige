package com.example.arcanebridge.client.render;

import com.example.arcanebridge.combat.MobArchetypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ShieldSuitLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public ShieldSuitLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       T entity, float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {

        if (entity instanceof Player || !entity.isAlive()) return;

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

        ResourceLocation texture = this.getTextureLocation(entity);
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));

        poseStack.pushPose();
        // Масштабирование строго относительно центра груди (Y = +0.75 в модельном пространстве)
        poseStack.translate(0.0D, 0.75D, 0.0D);
        poseStack.scale(pulse, pulse, pulse);
        poseStack.translate(0.0D, -0.75D, 0.0D);

        M model = this.getParentModel();
        model.renderToBuffer(poseStack, buffer, 15728880, OverlayTexture.NO_OVERLAY, r, g, b, alpha);

        poseStack.popPose();
    }
}