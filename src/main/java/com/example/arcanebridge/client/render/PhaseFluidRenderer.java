package com.example.arcanebridge.client.render;

import com.example.arcanebridge.block.PhaseFluidBlock;
import com.example.arcanebridge.block.entity.PhaseFluidBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class PhaseFluidRenderer implements BlockEntityRenderer<PhaseFluidBlockEntity> {

    private static final ResourceLocation GLYPH_TX =
            new ResourceLocation("arcane_bridge", "textures/vfx/phase_fluid_glyph_tx.png");
    private static final ResourceLocation GLYPH_RX =
            new ResourceLocation("arcane_bridge", "textures/vfx/phase_fluid_glyph_rx.png");
    private static final ResourceLocation GLYPH_LINES =
            new ResourceLocation("arcane_bridge", "textures/vfx/phase_fluid_glyph_lines.png");

    public PhaseFluidRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(PhaseFluidBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (state == null || !(state.getBlock() instanceof PhaseFluidBlock)) return;

        Direction.Axis axis = state.hasProperty(PhaseFluidBlock.AXIS) ? state.getValue(PhaseFluidBlock.AXIS) : Direction.Axis.Y;

        ms.pushPose();
        ms.translate(0.5D, 0.5D, 0.5D);

        switch (axis) {
            case X -> ms.mulPose(Axis.YP.rotationDegrees(90.0F));
            case Z -> {}
            case Y -> ms.mulPose(Axis.XP.rotationDegrees(90.0F));
        }

        float time = (be.getLevel() != null ? be.getLevel().getGameTime() : 0) + partialTicks;
        float pulse = 1.0F + (float) Math.sin(time * 0.08F) * 0.03F;
        ms.scale(pulse, pulse, pulse);

        float r = be.isReceiver ? 0.20F : 0.05F;
        float g = be.isReceiver ? 0.60F : 0.90F;
        float b = be.isReceiver ? 1.00F : 0.95F;
        float a = 0.95F;

        ResourceLocation capTexture = be.isReceiver ? GLYPH_RX : GLYPH_TX;

        Matrix4f posMat = ms.last().pose();
        Matrix3f normMat = ms.last().normal();

        float s = 0.44F;
        float halfLen = 0.48F;
        float yOffset = 0.44F;

        VertexConsumer capConsumer = buffer.getBuffer(RenderType.entityTranslucent(capTexture));
        drawGlyphCap(posMat, normMat, capConsumer, s, halfLen, r, g, b, a);
        drawGlyphCap(posMat, normMat, capConsumer, s, -halfLen, r, g, b, a);

        VertexConsumer wallConsumer = buffer.getBuffer(RenderType.entityTranslucent(GLYPH_LINES));
        for (int i = 0; i < 4; i++) {
            ms.pushPose();
            ms.mulPose(Axis.ZP.rotationDegrees(i * 90.0F));
            ms.translate(0.0D, yOffset, 0.0D);

            Matrix4f wallPosMat = ms.last().pose();
            Matrix3f wallNormMat = ms.last().normal();

            drawWallPanel(wallPosMat, wallNormMat, wallConsumer, s, halfLen, r, g, b, a * 0.90F);
            ms.popPose();
        }

        ms.popPose();
    }

    private void drawGlyphCap(Matrix4f posMat, Matrix3f normMat, VertexConsumer builder,
                              float s, float z, float r, float g, float b, float a) {
        int fullLight = 15728880;

        builder.vertex(posMat, -s, -s, z).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        builder.vertex(posMat,  s, -s, z).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        builder.vertex(posMat,  s,  s, z).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        builder.vertex(posMat, -s,  s, z).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();

        builder.vertex(posMat, -s,  s, z).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        builder.vertex(posMat,  s,  s, z).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        builder.vertex(posMat,  s, -s, z).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        builder.vertex(posMat, -s, -s, z).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
    }

    private void drawWallPanel(Matrix4f posMat, Matrix3f normMat, VertexConsumer builder,
                               float hw, float halfLen, float r, float g, float b, float a) {
        int fullLight = 15728880;

        builder.vertex(posMat, -hw, 0.0F, -halfLen).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        builder.vertex(posMat,  hw, 0.0F, -halfLen).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        builder.vertex(posMat,  hw, 0.0F,  halfLen).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        builder.vertex(posMat, -hw, 0.0F,  halfLen).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();

        builder.vertex(posMat, -hw, 0.0F,  halfLen).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        builder.vertex(posMat,  hw, 0.0F,  halfLen).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        builder.vertex(posMat,  hw, 0.0F, -halfLen).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        builder.vertex(posMat, -hw, 0.0F, -halfLen).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
    }
}