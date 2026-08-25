package com.example.arcanebridge.client.render;

import com.example.arcanebridge.block.PhaseRelayBlock;
import com.example.arcanebridge.block.entity.PhaseRelayBlockEntity;
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

public class PhaseRelayRenderer implements BlockEntityRenderer<PhaseRelayBlockEntity> {

    private static final ResourceLocation BLANK_TEX = 
            new ResourceLocation("minecraft", "textures/misc/white.png");

    public PhaseRelayRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(PhaseRelayBlockEntity relay, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        BlockState state = relay.getBlockState();
        if (!(state.getBlock() instanceof PhaseRelayBlock)) return;

        Direction.Axis axis = state.getValue(PhaseRelayBlock.AXIS);
        float speed = relay.getSpeed();

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);

        // 1. Ориентация вдоль оси вала (X, Y, Z)
        switch (axis) {
            case X -> poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            case Z -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case Y -> {}
        }

        // 2. Вращение вокруг оси со скоростью вала (при 0 RPM — легкое покачивание)
        float time = (relay.getLevel() != null ? relay.getLevel().getGameTime() : 0) + partialTick;
        float angle = (speed != 0) ? (time * (speed / 10.0F) * 3.0F) % 360.0F : (float) Math.sin(time * 0.05F) * 8.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));

        // 3. Пульсация дыхания эфира
        float pulse = 1.0F + (float) Math.sin(time * 0.1F) * 0.04F;
        poseStack.scale(pulse, pulse, pulse);

        Matrix4f posMat = poseStack.last().pose();
        Matrix3f normMat = poseStack.last().normal();

        // Цвета Hex: Розово-малиновый (TX) / Фиолетовый (RX)
        float r = relay.isReceiver ? 0.82F : 1.0F;
        float g = relay.isReceiver ? 0.28F : 0.45F;
        float b = relay.isReceiver ? 1.0F : 0.85F;

        RenderType translucentType = RenderType.entityTranslucentCull(BLANK_TEX);
        VertexConsumer faceConsumer = bufferSource.getBuffer(translucentType);

        // 4. Отрисовка полупрозрачных граней кристалла (радиус 0.55 перекрывает вал 0.25)
        drawHexGem(posMat, normMat, faceConsumer, 0.55F, 0.18F, 0.45F, r, g, b, 0.55F);

        // Принудительный сброс буфера граней для шейдеров
        if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(translucentType);
        }

        // 5. Отрисовка неонового каркаса ребер (Wireframe)
        RenderType lineType = RenderType.lines();
        VertexConsumer lineConsumer = bufferSource.getBuffer(lineType);
        drawHexGemWireframe(posMat, normMat, lineConsumer, 0.56F, 0.19F, 0.46F,
                Math.min(1.0F, r * 1.3F), Math.min(1.0F, g * 1.4F), Math.min(1.0F, b * 1.3F), 0.95F);

        if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(lineType);
        }

        poseStack.popPose();
    }

    /**
     * Отрисовка 3D-ограненного гексагонального кристалла (верхний конус + средний пояс + нижний конус)
     */
    private void drawHexGem(Matrix4f mat, Matrix3f norm, VertexConsumer b,
                            float radius, float beltHalfH, float tipH,
                            float r, float g, float bCol, float a) {
        float[] hx = new float[6];
        float[] hz = new float[6];

        for (int i = 0; i < 6; i++) {
            double rad = Math.toRadians(i * 60.0);
            hx[i] = (float) (Math.cos(rad) * radius);
            hz[i] = (float) (Math.sin(rad) * radius);
        }

        for (int i = 0; i < 6; i++) {
            int next = (i + 1) % 6;

            // 1. Верхние 6 треугольных граней (с вершиной в topTip)
            addTriDoubleSided(b, mat, norm,
                    0.0F, tipH, 0.0F,
                    hx[i], beltHalfH, hz[i],
                    hx[next], beltHalfH, hz[next],
                    r, g, bCol, a);

            // 2. Средние 6 прямоугольных граней пояса
            addQuadDoubleSided(b, mat, norm,
                    hx[i], -beltHalfH, hz[i],
                    hx[next], -beltHalfH, hz[next],
                    hx[next], beltHalfH, hz[next],
                    hx[i], beltHalfH, hz[i],
                    r, g, bCol, a);

            // 3. Нижние 6 треугольных граней (с вершиной в bottomTip)
            addTriDoubleSided(b, mat, norm,
                    0.0F, -tipH, 0.0F,
                    hx[next], -beltHalfH, hz[next],
                    hx[i], -beltHalfH, hz[i],
                    r, g, bCol, a);
        }
    }

    private void drawHexGemWireframe(Matrix4f mat, Matrix3f norm, VertexConsumer b,
                                     float radius, float beltHalfH, float tipH,
                                     float r, float g, float bCol, float a) {
        float[] hx = new float[6];
        float[] hz = new float[6];

        for (int i = 0; i < 6; i++) {
            double rad = Math.toRadians(i * 60.0);
            hx[i] = (float) (Math.cos(rad) * radius);
            hz[i] = (float) (Math.sin(rad) * radius);
        }

        for (int i = 0; i < 6; i++) {
            int next = (i + 1) % 6;

            // Ребра к верхнему пику
            addLine(b, mat, norm, 0.0F, tipH, 0.0F, hx[i], beltHalfH, hz[i], r, g, bCol, a);
            // Верхний пояс
            addLine(b, mat, norm, hx[i], beltHalfH, hz[i], hx[next], beltHalfH, hz[next], r, g, bCol, a);
            // Вертикальные ребра пояса
            addLine(b, mat, norm, hx[i], beltHalfH, hz[i], hx[i], -beltHalfH, hz[i], r, g, bCol, a);
            // Нижний пояс
            addLine(b, mat, norm, hx[i], -beltHalfH, hz[i], hx[next], -beltHalfH, hz[next], r, g, bCol, a);
            // Ребра к нижнему пику
            addLine(b, mat, norm, 0.0F, -tipH, 0.0F, hx[i], -beltHalfH, hz[i], r, g, bCol, a);
        }
    }

    private void addQuadDoubleSided(VertexConsumer b, Matrix4f mat, Matrix3f norm,
                                   float x1, float y1, float z1,
                                   float x2, float y2, float z2,
                                   float x3, float y3, float z3,
                                   float x4, float y4, float z4,
                                   float r, float g, float bCol, float a) {
        addV(b, mat, norm, x1, y1, z1, r, g, bCol, a);
        addV(b, mat, norm, x2, y2, z2, r, g, bCol, a);
        addV(b, mat, norm, x3, y3, z3, r, g, bCol, a);
        addV(b, mat, norm, x4, y4, z4, r, g, bCol, a);

        addV(b, mat, norm, x4, y4, z4, r, g, bCol, a);
        addV(b, mat, norm, x3, y3, z3, r, g, bCol, a);
        addV(b, mat, norm, x2, y2, z2, r, g, bCol, a);
        addV(b, mat, norm, x1, y1, z1, r, g, bCol, a);
    }

    private void addTriDoubleSided(VertexConsumer b, Matrix4f mat, Matrix3f norm,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2,
                                  float x3, float y3, float z3,
                                  float r, float g, float bCol, float a) {
        addV(b, mat, norm, x1, y1, z1, r, g, bCol, a);
        addV(b, mat, norm, x2, y2, z2, r, g, bCol, a);
        addV(b, mat, norm, x3, y3, z3, r, g, bCol, a);
        addV(b, mat, norm, x3, y3, z3, r, g, bCol, a);

        addV(b, mat, norm, x3, y3, z3, r, g, bCol, a);
        addV(b, mat, norm, x2, y2, z2, r, g, bCol, a);
        addV(b, mat, norm, x1, y1, z1, r, g, bCol, a);
        addV(b, mat, norm, x1, y1, z1, r, g, bCol, a);
    }

    private void addLine(VertexConsumer b, Matrix4f mat, Matrix3f norm,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float r, float g, float bCol, float a) {
        b.vertex(mat, x1, y1, z1).color(r, g, bCol, a).normal(norm, 0, 1, 0).endVertex();
        b.vertex(mat, x2, y2, z2).color(r, g, bCol, a).normal(norm, 0, 1, 0).endVertex();
    }

    private void addV(VertexConsumer b, Matrix4f mat, Matrix3f norm,
                      float x, float y, float z,
                      float r, float g, float bCol, float a) {
        b.vertex(mat, x, y, z)
                .color(r, g, bCol, a)
                .uv(0.5F, 0.5F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880) // 0xF000F0 (максимальная яркость свечения)
                .normal(norm, 0, 1, 0)
                .endVertex();
    }
}