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

    private static final ResourceLocation TEX_END_SKY =
            new ResourceLocation("minecraft", "textures/environment/end_sky.png");
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

        // 1. Поворот по оси трубы Create
        switch (axis) {
            case X -> ms.mulPose(Axis.YP.rotationDegrees(90.0F));
            case Z -> {}
            case Y -> ms.mulPose(Axis.XP.rotationDegrees(90.0F));
        }

        float time = (be.getLevel() != null ? be.getLevel().getGameTime() : 0) + partialTicks;

        // -------------------------------------------------------------------------
        // 🌌 1. СТАТИЧНАЯ НЕПРОЗРАЧНАЯ ЗАГЛУШКА ЭНДА (СТРОГО ВНУТРИ ОТВЕРСТИЯ ТРУБЫ)
        // -------------------------------------------------------------------------
        float portalHalfSize = 0.245F; // Точный размер внутреннего отверстия (8x8 пикселей)
        float portalDepth = 0.496F;    // Расположение на срезе торца

        VertexConsumer solidVoidConsumer = buffer.getBuffer(RenderType.entitySolid(TEX_END_SKY));
        drawFixedPortalBore(ms.last().pose(), ms.last().normal(), solidVoidConsumer, portalHalfSize, portalDepth);
        drawFixedPortalBore(ms.last().pose(), ms.last().normal(), solidVoidConsumer, portalHalfSize, -portalDepth);

        // -------------------------------------------------------------------------
        // 🔮 2. ЕДИНЫЙ ВРАЩАЮЩИЙСЯ 3D-КАРКАС (ГЛИФЫ + БОКОВЫЕ ЛИНИИ)
        // -------------------------------------------------------------------------
        float pulse = 1.0F + (float) Math.sin(time * 0.08F) * 0.025F;
        float r = be.isReceiver ? 0.20F : 0.05F;
        float g = be.isReceiver ? 0.60F : 0.90F;
        float b = be.isReceiver ? 1.00F : 0.95F;
        float a = 0.95F;

        float cageSize = 0.34F * pulse; // Полуширина каркаса и торцевых глифов
        float cageHalfLen = 0.501F;     // Полная длина каркаса от торца до торца
        float rotAngle = time * 0.6F;   // Скорость вращения всей структуры

        ms.pushPose();
        // Вращаем весь каркас целиком как единое твердое тело
        ms.mulPose(Axis.ZP.rotationDegrees(rotAngle));

        Matrix4f cagePosMat = ms.last().pose();
        Matrix3f cageNormMat = ms.last().normal();

        // А. Торцевые светящиеся глифы (+Z и -Z)
        ResourceLocation glyphTexture = be.isReceiver ? GLYPH_RX : GLYPH_TX;
        VertexConsumer glyphConsumer = buffer.getBuffer(RenderType.entityTranslucent(glyphTexture));

        drawEndGlyph(cagePosMat, cageNormMat, glyphConsumer, cageSize, cageHalfLen, r, g, b, a, true);
        drawEndGlyph(cagePosMat, cageNormMat, glyphConsumer, cageSize, -cageHalfLen, r, g, b, a, false);

        // Б. 4 боковые грани, соединяющие углы глифов
        VertexConsumer wallConsumer = buffer.getBuffer(RenderType.entityTranslucent(GLYPH_LINES));
        for (int i = 0; i < 4; i++) {
            ms.pushPose();
            ms.mulPose(Axis.ZP.rotationDegrees(i * 90.0F));
            ms.translate(0.0D, cageSize, 0.0D);

            Matrix4f wallPosMat = ms.last().pose();
            Matrix3f wallNormMat = ms.last().normal();

            drawWallPanel(wallPosMat, wallNormMat, wallConsumer, cageSize, cageHalfLen, r, g, b, a * 0.90F);
            ms.popPose();
        }

        ms.popPose(); // Возврат из общего вращения каркаса
        ms.popPose(); // Возврат из базовой трансформации
    }

    /**
     * Неподвижная заглушка неба Энда (Opaque / Solid, отсекает видимость внутренней геометрии)
     */
    private void drawFixedPortalBore(Matrix4f posMat, Matrix3f normMat, VertexConsumer builder, float s, float z) {
        int fullLight = 15728880;

        builder.vertex(posMat, -s, -s, z).color(0.12F, 0.02F, 0.22F, 1.0F).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        builder.vertex(posMat,  s, -s, z).color(0.12F, 0.02F, 0.22F, 1.0F).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        builder.vertex(posMat,  s,  s, z).color(0.12F, 0.02F, 0.22F, 1.0F).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        builder.vertex(posMat, -s,  s, z).color(0.12F, 0.02F, 0.22F, 1.0F).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();

        // Обратная сторона для взгляда изнутри
        builder.vertex(posMat, -s,  s, z).color(0.12F, 0.02F, 0.22F, 1.0F).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, -1.0F).endVertex();
        builder.vertex(posMat,  s,  s, z).color(0.12F, 0.02F, 0.22F, 1.0F).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, -1.0F).endVertex();
        builder.vertex(posMat,  s, -s, z).color(0.12F, 0.02F, 0.22F, 1.0F).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, -1.0F).endVertex();
        builder.vertex(posMat, -s, -s, z).color(0.12F, 0.02F, 0.22F, 1.0F).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, -1.0F).endVertex();
    }

    /**
     * Торцевой светящийся глиф с корректным направлением обхода вершин
     */
    private void drawEndGlyph(Matrix4f posMat, Matrix3f normMat, VertexConsumer builder,
                              float s, float z, float r, float g, float b, float a, boolean isFront) {
        int fullLight = 15728880;

        if (isFront) {
            builder.vertex(posMat, -s, -s, z).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
            builder.vertex(posMat,  s, -s, z).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
            builder.vertex(posMat,  s,  s, z).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
            builder.vertex(posMat, -s,  s, z).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        } else {
            builder.vertex(posMat,  s, -s, z).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, -1.0F).endVertex();
            builder.vertex(posMat, -s, -s, z).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, -1.0F).endVertex();
            builder.vertex(posMat, -s,  s, z).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, -1.0F).endVertex();
            builder.vertex(posMat,  s,  s, z).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, -1.0F).endVertex();
        }
    }

    /**
     * Боковая грань призмы (соединяет торцы от -cageHalfLen до +cageHalfLen)
     */
    private void drawWallPanel(Matrix4f posMat, Matrix3f normMat, VertexConsumer builder,
                               float hw, float halfLen, float r, float g, float b, float a) {
        int fullLight = 15728880;

        // Внешняя плоскость
        builder.vertex(posMat, -hw, 0.0F, -halfLen).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 1.0F, 0.0F).endVertex();
        builder.vertex(posMat,  hw, 0.0F, -halfLen).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 1.0F, 0.0F).endVertex();
        builder.vertex(posMat,  hw, 0.0F,  halfLen).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 1.0F, 0.0F).endVertex();
        builder.vertex(posMat, -hw, 0.0F,  halfLen).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 1.0F, 0.0F).endVertex();

        // Внутренняя плоскость
        builder.vertex(posMat, -hw, 0.0F,  halfLen).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, -1.0F, 0.0F).endVertex();
        builder.vertex(posMat,  hw, 0.0F,  halfLen).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, -1.0F, 0.0F).endVertex();
        builder.vertex(posMat,  hw, 0.0F, -halfLen).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, -1.0F, 0.0F).endVertex();
        builder.vertex(posMat, -hw, 0.0F, -halfLen).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, -1.0F, 0.0F).endVertex();
    }
}