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

    private static final ResourceLocation AMETHYST_TEX = 
            new ResourceLocation("minecraft", "textures/block/amethyst_block.png");

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

        // 1. Поворот системы координат по оси вала
        switch (axis) {
            case X -> poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            case Z -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case Y -> {}
        }

        // 2. Вращение гексагона вместе с валом
        float time = (relay.getLevel() != null ? relay.getLevel().getGameTime() : 0) + partialTick;
        float angle = (time * (speed / 10.0F) * 3.0F) % 360.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));

        // 3. Пульсация эфирного поля
        float pulse = 1.0F + (float) Math.sin(time * 0.1F) * 0.04F;
        poseStack.scale(pulse, pulse, pulse);

        Matrix4f posMat = poseStack.last().pose();
        Matrix3f normMat = poseStack.last().normal();

        // Цвета Hex: Розовый (TX) / Фиолетовый (RX)
        float r = relay.isReceiver ? 0.90F : 1.0F;
        float g = relay.isReceiver ? 0.30F : 0.60F;
        float b = relay.isReceiver ? 1.0F : 0.85F;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(AMETHYST_TEX));

        // 4. Отрисовка объемного гексагонального кристалла (радиус 0.48, высота 0.65)
        drawHexPrismDoubleSided(posMat, normMat, consumer, 0.48F, 0.65F, r, g, b, 0.65F);

        // Внутреннее светящееся ядро (противоход)
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-angle * 1.5F));
        drawHexPrismDoubleSided(poseStack.last().pose(), poseStack.last().normal(), consumer, 0.28F, 0.45F, 1.0F, 1.0F, 1.0F, 0.85F);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void drawHexPrismDoubleSided(Matrix4f matrix, Matrix3f normal, VertexConsumer builder,
                                         float radius, float height, float r, float g, float b, float a) {
        float halfH = height / 2.0F;
        float[] hx = new float[6];
        float[] hz = new float[6];

        for (int i = 0; i < 6; i++) {
            double rad = Math.toRadians(i * 60.0);
            hx[i] = (float) (Math.cos(rad) * radius);
            hz[i] = (float) (Math.sin(rad) * radius);
        }

        // 6 боковых граней (двухсторонние)
        for (int i = 0; i < 6; i++) {
            int next = (i + 1) % 6;
            // Лицевая сторона
            addV(builder, matrix, normal, hx[i], -halfH, hz[i], 0.0F, 0.0F, r, g, b, a);
            addV(builder, matrix, normal, hx[next], -halfH, hz[next], 1.0F, 0.0F, r, g, b, a);
            addV(builder, matrix, normal, hx[next], halfH, hz[next], 1.0F, 1.0F, r, g, b, a);
            addV(builder, matrix, normal, hx[i], halfH, hz[i], 0.0F, 1.0F, r, g, b, a);

            // Обратная сторона
            addV(builder, matrix, normal, hx[i], halfH, hz[i], 0.0F, 1.0F, r, g, b, a);
            addV(builder, matrix, normal, hx[next], halfH, hz[next], 1.0F, 1.0F, r, g, b, a);
            addV(builder, matrix, normal, hx[next], -halfH, hz[next], 1.0F, 0.0F, r, g, b, a);
            addV(builder, matrix, normal, hx[i], -halfH, hz[i], 0.0F, 0.0F, r, g, b, a);
        }

        // Верхняя и нижняя крышки гексагона
        for (int i = 1; i < 5; i++) {
            // Верх
            addV(builder, matrix, normal, hx[0], halfH, hz[0], 0.5F, 0.5F, r, g, b, a);
            addV(builder, matrix, normal, hx[i], halfH, hz[i], 0.0F, 0.0F, r, g, b, a);
            addV(builder, matrix, normal, hx[i + 1], halfH, hz[i + 1], 1.0F, 0.0F, r, g, b, a);
            addV(builder, matrix, normal, hx[0], halfH, hz[0], 0.5F, 0.5F, r, g, b, a);

            // Низ
            addV(builder, matrix, normal, hx[0], -halfH, hz[0], 0.5F, 0.5F, r, g, b, a);
            addV(builder, matrix, normal, hx[i + 1], -halfH, hz[i + 1], 1.0F, 0.0F, r, g, b, a);
            addV(builder, matrix, normal, hx[i], -halfH, hz[i], 0.0F, 0.0F, r, g, b, a);
            addV(builder, matrix, normal, hx[0], -halfH, hz[0], 0.5F, 0.5F, r, g, b, a);
        }
    }

    private void addV(VertexConsumer builder, Matrix4f mat, Matrix3f norm,
                      float x, float y, float z, float u, float v,
                      float r, float g, float b, float a) {
        builder.vertex(mat, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880) // Максимальная яркость свечения (0xF000F0)
                .normal(norm, 0, 1, 0)
                .endVertex();
    }
}