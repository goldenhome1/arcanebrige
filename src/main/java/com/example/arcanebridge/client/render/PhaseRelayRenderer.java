package com.example.arcanebridge.client.render;

import com.example.arcanebridge.block.PhaseRelayBlock;
import com.example.arcanebridge.block.entity.PhaseRelayBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class PhaseRelayRenderer extends KineticBlockEntityRenderer<PhaseRelayBlockEntity> {

        private static final ResourceLocation GLYPH_TX =
            new ResourceLocation("arcane_bridge", "textures/vfx/phase_glyph_tx.png");
    private static final ResourceLocation GLYPH_RX =
            new ResourceLocation("arcane_bridge", "textures/vfx/phase_glyph_rx.png");
    private static final ResourceLocation GLYPH_LINES =
            new ResourceLocation("arcane_bridge", "textures/vfx/phase_glyph_lines.png");

    public PhaseRelayRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(PhaseRelayBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        BlockState state = be.getBlockState();
        if (state == null || !(state.getBlock() instanceof PhaseRelayBlock)) return;

        // 1. Отрисовка рабочего вала Create
        renderRotatingBuffer(be, getRotatedModel(be, state), ms, buffer.getBuffer(RenderType.solid()), light);

        // 2. Отрисовка объёмной 3D-призмы глифа Hex Casting
        renderHexGlyphPrism(be, state, partialTicks, ms, buffer);
    }

    private void renderHexGlyphPrism(PhaseRelayBlockEntity be, BlockState state, float partialTicks,
                                     PoseStack ms, MultiBufferSource buffer) {
        Direction.Axis axis = state.getValue(PhaseRelayBlock.AXIS);
        float speed = be.getSpeed();

        ms.pushPose();
        ms.translate(0.5D, 0.5D, 0.5D);

        // Ориентация призмы вдоль оси вращения вала
        switch (axis) {
            case X -> ms.mulPose(Axis.YP.rotationDegrees(90.0F));
            case Z -> {}
            case Y -> ms.mulPose(Axis.XP.rotationDegrees(90.0F));
        }

        // Замедленное вращение (в 25 раз медленнее оборотов вала)
        float time = (be.getLevel() != null ? be.getLevel().getGameTime() : 0) + partialTicks;
        float angle = (speed != 0) ? (time * (speed / 250.0F) * 3.0F) % 360.0F : (float) Math.sin(time * 0.03F) * 5.0F;
        ms.mulPose(Axis.ZP.rotationDegrees(angle));

        // Эфирное дыхание
        float pulse = 1.0F + (float) Math.sin(time * 0.1F) * 0.04F;
        ms.scale(pulse, pulse, pulse);

        // Цветовой профиль: Розовый (TX) / Фиолетовый (RX)
        ResourceLocation texture = be.isReceiver ? GLYPH_RX : GLYPH_TX;
        float r = be.isReceiver ? 0.75F : 1.0F;
        float g = be.isReceiver ? 0.35F : 0.30F;
        float b = be.isReceiver ? 1.0F : 0.75F;
        float a = 0.90F;

        Matrix4f posMat = ms.last().pose();
        Matrix3f normMat = ms.last().normal();

        float size = 0.65F;    // Размер глифа
        float halfLength = 0.48F; // Смещение торцов вдоль вала (+/- Z)

                // --- ПРОХОД 1: Торцевые глифы (Передний и Задний) ---
        VertexConsumer glyphConsumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
        drawGlyphCap(posMat, normMat, glyphConsumer, size, halfLength, r, g, b, a);
        drawGlyphCap(posMat, normMat, glyphConsumer, size, -halfLength, r, g, b, a);

        // --- ПРОХОД 2: 6 вращающихся плоскостей продольных линий (все 12 граней) ---
        VertexConsumer lineConsumer = buffer.getBuffer(RenderType.entityTranslucent(GLYPH_LINES));
        for (int i = 0; i < 6; i++) {
            ms.pushPose();
            ms.mulPose(Axis.ZP.rotationDegrees(i * 60.0F));
            Matrix4f linePosMat = ms.last().pose();
            Matrix3f lineNormMat = ms.last().normal();
            drawConnectingRibs(linePosMat, lineNormMat, lineConsumer, size, halfLength, r, g, b, a * 0.90F);
            ms.popPose();
        }

        ms.popPose();
    }

    private void drawGlyphCap(Matrix4f posMat, Matrix3f normMat, VertexConsumer builder,
                              float s, float z, float r, float g, float b, float a) {
        int fullLight = 15728880;

        builder.vertex(posMat, -s, -s, z).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 0, 1).endVertex();
        builder.vertex(posMat,  s, -s, z).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 0, 1).endVertex();
        builder.vertex(posMat,  s,  s, z).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 0, 1).endVertex();
        builder.vertex(posMat, -s,  s, z).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 0, 1).endVertex();

        builder.vertex(posMat, -s,  s, z).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 0, -1).endVertex();
        builder.vertex(posMat,  s,  s, z).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 0, -1).endVertex();
        builder.vertex(posMat,  s, -s, z).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 0, -1).endVertex();
        builder.vertex(posMat, -s, -s, z).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 0, -1).endVertex();
    }

    private void drawConnectingRibs(Matrix4f posMat, Matrix3f normMat, VertexConsumer builder,
                                    float s, float halfLen, float r, float g, float b, float a) {
        int fullLight = 15728880;

        // Лицевая плоскость (протянута вдоль оси Z от -halfLen до +halfLen)
        builder.vertex(posMat, -s, 0.0F, -halfLen).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 1, 0).endVertex();
        builder.vertex(posMat,  s, 0.0F, -halfLen).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 1, 0).endVertex();
        builder.vertex(posMat,  s, 0.0F,  halfLen).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 1, 0).endVertex();
        builder.vertex(posMat, -s, 0.0F,  halfLen).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 1, 0).endVertex();

        // Обратная плоскость
        builder.vertex(posMat, -s, 0.0F,  halfLen).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, -1, 0).endVertex();
        builder.vertex(posMat,  s, 0.0F,  halfLen).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, -1, 0).endVertex();
        builder.vertex(posMat,  s, 0.0F, -halfLen).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, -1, 0).endVertex();
        builder.vertex(posMat, -s, 0.0F, -halfLen).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, -1, 0).endVertex();
    }

    @Override
    protected SuperByteBuffer getRotatedModel(PhaseRelayBlockEntity be, BlockState state) {
        Block shaftBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create", "shaft"));
        if (shaftBlock != null) {
            BlockState shaftState = shaftBlock.defaultBlockState()
                    .setValue(PhaseRelayBlock.AXIS, state.getValue(PhaseRelayBlock.AXIS));
            return CachedBuffers.block(shaftState);
        }
        return CachedBuffers.block(state);
    }
}