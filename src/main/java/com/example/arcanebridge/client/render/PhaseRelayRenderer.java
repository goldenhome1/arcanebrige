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
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class PhaseRelayRenderer implements BlockEntityRenderer<PhaseRelayBlockEntity> {

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

        // 1. Ориентация по оси вала (X, Y или Z)
        switch (axis) {
            case X -> poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            case Z -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case Y -> {}
        }

        // 2. Вращение гексагонального кристалла синхронно с RPM вала
        float time = (relay.getLevel() != null ? relay.getLevel().getGameTime() : 0) + partialTick;
        float angle = (time * (speed / 10.0F) * 3.0F) % 360.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));

        // 3. Пульсация эфирного поля
        float pulse = 1.0F + (float) Math.sin(time * 0.12F) * 0.05F;
        poseStack.scale(pulse, pulse, pulse);

        Matrix4f posMat = poseStack.last().pose();
        Matrix3f normMat = poseStack.last().normal();

        // 4. Цветовая палитра Hex Casting: Розово-пурпурная (TX) / Насыщенная фиолетовая (RX)
        float r = relay.isReceiver ? 0.95F : 1.0F;
        float g = relay.isReceiver ? 0.30F : 0.65F;
        float b = relay.isReceiver ? 0.85F : 0.90F;

        // Внутреннее полупрозрачное тело призмы
        VertexConsumer transConsumer = bufferSource.getBuffer(RenderType.translucentNoCrumbling());
        drawHexPrism(posMat, normMat, transConsumer, 0.42F, 0.55F, r, g, b, 0.45F, 15728880);

        // Внешний контур ребер (wireframe каркас)
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        drawHexWireframe(posMat, normMat, lineConsumer, 0.43F, 0.56F, r, g, b, 0.90F);

        poseStack.popPose();
    }

    private void drawHexPrism(Matrix4f matrix, Matrix3f normal, VertexConsumer builder,
                              float radius, float height, float r, float g, float b, float a, int light) {
        float halfH = height / 2.0F;
        float[] hx = new float[6];
        float[] hz = new float[6];

        for (int i = 0; i < 6; i++) {
            double rad = Math.toRadians(i * 60.0);
            hx[i] = (float) (Math.cos(rad) * radius);
            hz[i] = (float) (Math.sin(rad) * radius);
        }

        // 6 боковых граней гексагона
        for (int i = 0; i < 6; i++) {
            int next = (i + 1) % 6;
            builder.vertex(matrix, hx[i], -halfH, hz[i]).color(r, g, b, a).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
            builder.vertex(matrix, hx[next], -halfH, hz[next]).color(r, g, b, a).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
            builder.vertex(matrix, hx[next], halfH, hz[next]).color(r, g, b, a).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
            builder.vertex(matrix, hx[i], halfH, hz[i]).color(r, g, b, a).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
        }

        // Верхняя и нижняя крышки (Hexagon caps)
        for (int i = 1; i < 5; i++) {
            builder.vertex(matrix, hx[0], halfH, hz[0]).color(r, g, b, a).uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
            builder.vertex(matrix, hx[i], halfH, hz[i]).color(r, g, b, a).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
            builder.vertex(matrix, hx[i + 1], halfH, hz[i + 1]).color(r, g, b, a).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
            builder.vertex(matrix, hx[0], halfH, hz[0]).color(r, g, b, a).uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();

            builder.vertex(matrix, hx[0], -halfH, hz[0]).color(r, g, b, a).uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, -1, 0).endVertex();
            builder.vertex(matrix, hx[i + 1], -halfH, hz[i + 1]).color(r, g, b, a).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, -1, 0).endVertex();
            builder.vertex(matrix, hx[i], -halfH, hz[i]).color(r, g, b, a).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, -1, 0).endVertex();
            builder.vertex(matrix, hx[0], -halfH, hz[0]).color(r, g, b, a).uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, -1, 0).endVertex();
        }
    }

    private void drawHexWireframe(Matrix4f matrix, Matrix3f normal, VertexConsumer builder,
                                  float radius, float height, float r, float g, float b, float a) {
        float halfH = height / 2.0F;
        float[] hx = new float[6];
        float[] hz = new float[6];

        for (int i = 0; i < 6; i++) {
            double rad = Math.toRadians(i * 60.0);
            hx[i] = (float) (Math.cos(rad) * radius);
            hz[i] = (float) (Math.sin(rad) * radius);
        }

        for (int i = 0; i < 6; i++) {
            int next = (i + 1) % 6;
            // Верхний и нижний периметры
            builder.vertex(matrix, hx[i], halfH, hz[i]).color(r, g, b, a).normal(normal, 0, 1, 0).endVertex();
            builder.vertex(matrix, hx[next], halfH, hz[next]).color(r, g, b, a).normal(normal, 0, 1, 0).endVertex();

            builder.vertex(matrix, hx[i], -halfH, hz[i]).color(r, g, b, a).normal(normal, 0, -1, 0).endVertex();
            builder.vertex(matrix, hx[next], -halfH, hz[next]).color(r, g, b, a).normal(normal, 0, -1, 0).endVertex();

            // Вертикальные стойки
            builder.vertex(matrix, hx[i], -halfH, hz[i]).color(r, g, b, a).normal(normal, 0, 1, 0).endVertex();
            builder.vertex(matrix, hx[i], halfH, hz[i]).color(r, g, b, a).normal(normal, 0, 1, 0).endVertex();
        }
    }
}