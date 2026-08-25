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

        // 1. Отрисовка вала Create
        renderRotatingBuffer(be, getRotatedModel(be, state), ms, buffer.getBuffer(RenderType.solid()), light);

        // 2. Отрисовка 3D-рукава глифа Hex Casting
        renderHexGlyphPrism(be, state, partialTicks, ms, buffer);
    }

    private void renderHexGlyphPrism(PhaseRelayBlockEntity be, BlockState state, float partialTicks,
                                     PoseStack ms, MultiBufferSource buffer) {
        Direction.Axis axis = state.getValue(PhaseRelayBlock.AXIS);
        float speed = be.getSpeed();

        ms.pushPose();
        ms.translate(0.5D, 0.5D, 0.5D);

        // Ориентация туннеля вдоль продольной оси вала
        switch (axis) {
            case X -> ms.mulPose(Axis.YP.rotationDegrees(90.0F));
            case Z -> {}
            case Y -> ms.mulPose(Axis.XP.rotationDegrees(90.0F));
        }

        // Замедленное вращение каркаса (в 25 раз медленнее вала)
        float time = (be.getLevel() != null ? be.getLevel().getGameTime() : 0) + partialTicks;
        float angle = (speed != 0) ? (time * (speed / 250.0F) * 3.0F) % 360.0F : (float) Math.sin(time * 0.03F) * 5.0F;
        ms.mulPose(Axis.ZP.rotationDegrees(angle));

        // Пульсация эфира
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

        float size = 0.65F;       // Размер торцевых крышек
        float halfLen = 0.48F;    // Полудлина (Z)
        float yOffset = 0.259F;   // Радиальный вынос граней от центра
        float halfWidth = 0.152F; // Полуширина грани (X)

        // --- ПРОХОД 1: Торцевые крышки (+Z и -Z) ---
        VertexConsumer capConsumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
        drawGlyphCap(posMat, normMat, capConsumer, size, halfLen, r, g, b, a);
        drawGlyphCap(posMat, normMat, capConsumer, size, -halfLen, r, g, b, a);

        // --- ПРОХОД 2: 6 касательных граней по периметру (полый тоннель) ---
        VertexConsumer wallConsumer = buffer.getBuffer(RenderType.entityTranslucent(GLYPH_LINES));
        for (int i = 0; i < 6; i++) {
            ms.pushPose();
            ms.mulPose(Axis.ZP.rotationDegrees(i * 60.0F));
            ms.translate(0.0D, yOffset, 0.0D);

            Matrix4f wallPosMat = ms.last().pose();
            Matrix3f wallNormMat = ms.last().normal();

            drawWallPanel(wallPosMat, wallNormMat, wallConsumer, halfWidth, halfLen, r, g, b, a * 0.95F);
            ms.popPose();
        }

        ms.popPose();
    }

    private void drawGlyphCap(Matrix4f posMat, Matrix3f normMat, VertexConsumer builder,
                              float s, float z, float r, float g, float b, float a) {
        int fullLight = 15728880;

        // Лицевая сторона
        builder.vertex(posMat, -s, -s, z).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 0, 1).endVertex();
        builder.vertex(posMat,  s, -s, z).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 0, 1).endVertex();
        builder.vertex(posMat,  s,  s, z).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 0, 1).endVertex();
        builder.vertex(posMat, -s,  s, z).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 0, 1).endVertex();

        // Обратная сторона
        builder.vertex(posMat, -s,  s, z).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 0, -1).endVertex();
        builder.vertex(posMat,  s,  s, z).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 0, -1).endVertex();
        builder.vertex(posMat,  s, -s, z).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 0, -1).endVertex();
        builder.vertex(posMat, -s, -s, z).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 0, -1).endVertex();
    }

    private void drawWallPanel(Matrix4f posMat, Matrix3f normMat, VertexConsumer builder,
                               float hw, float halfLen, float r, float g, float b, float a) {
        int fullLight = 15728880;

        // Внешняя сторона панели
        builder.vertex(posMat, -hw, 0.0F, -halfLen).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 1, 0).endVertex();
        builder.vertex(posMat,  hw, 0.0F, -halfLen).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 1, 0).endVertex();
        builder.vertex(posMat,  hw, 0.0F,  halfLen).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 1, 0).endVertex();
        builder.vertex(posMat, -hw, 0.0F,  halfLen).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, 1, 0).endVertex();

        // Внутренняя сторона панели
        builder.vertex(posMat, -hw, 0.0F,  halfLen).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, -1, 0).endVertex();
        builder.vertex(posMat,  hw, 0.0F,  halfLen).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, -1, 0).endVertex();
        builder.vertex(posMat,  hw, 0.0F, -halfLen).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, -1, 0).endVertex();
        builder.vertex(posMat, -hw, 0.0F, -halfLen).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0, -1, 0).endVertex();
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