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

    private static final ResourceLocation SHIELD_RUNES_TEX =
            new ResourceLocation("arcane_bridge", "textures/vfx/shield_runes.png");

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

        // Цветовые профили неонового свечения матрицы
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
                r = 0.20F; g = 0.85F; b = 1.00F; // Лазурный
            }
        }

        float time = entity.tickCount + partialTick;

        // Плавное смещение UV-координат (эффект струящейся маны по телу)
        float uOffset = (time * 0.006F) % 1.0F;
        float vOffset = (time * 0.010F) % 1.0F;

        // Аддитивный шейдерный слой энерго-поля (как Charged Creeper)
        RenderType energySwirlType = RenderType.energySwirl(SHIELD_RUNES_TEX, uOffset, vOffset);
        VertexConsumer buffer = bufferSource.getBuffer(energySwirlType);

        // Пульсация объема и реакция на удар (Hit Flash)
        float pulse = 1.035F + (float) Math.sin(time * 0.08F) * 0.010F;
        float alpha = 0.85F;
        if (entity.hurtTime > 0) {
            pulse += 0.025F;
            r = Math.min(1.0F, r + 0.4F);
            g = Math.min(1.0F, g + 0.4F);
            b = Math.min(1.0F, b + 0.4F);
        }

        double midY = 1.501D - (entity.getBbHeight() * 0.5D);

        poseStack.pushPose();
        poseStack.translate(0.0D, midY, 0.0D);
        poseStack.scale(pulse, pulse, pulse);
        poseStack.translate(0.0D, -midY, 0.0D);

        M model = this.getParentModel();
        // Эмиссионный полнояркостный свет для шейдеров (15728880)
        model.renderToBuffer(poseStack, buffer, 15728880, OverlayTexture.NO_OVERLAY, r, g, b, alpha);

        poseStack.popPose();
    }
}