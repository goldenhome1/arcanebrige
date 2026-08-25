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

    // 100% валидная текстура маяка из ванильного Minecraft
    private static final ResourceLocation BEAM_TEX = 
            new ResourceLocation("minecraft", "textures/entity/beacon_beam.png");

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

        // 1. Ориентация вдоль вала
        switch (axis) {
            case X -> poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            case Z -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case Y -> {}
        }

        // 2. Вращение: при наличии скорости — по оборотам вала, в покое — плавное покачивание
        float time = (relay.getLevel() != null ? relay.getLevel().getGameTime() : 0) + partialTick;
        float angle = (speed != 0) ? (time * (speed / 10.0F) * 3.0F) % 360.0F : (float) Math.sin(time * 0.05F) * 12.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));

        // 3. Эфирная пульсация
        float pulse = 1.0F + (float) Math.sin(time * 0.08F) * 0.04F;
        poseStack.scale(pulse, pulse, pulse);

        // 4. Цвета Hex: Розовый/Пурпурный (TX) и Фиолетовый (RX)
        float r = relay.isReceiver ? 0.85F : 1.0F;
        float g = relay.isReceiver ? 0.25F : 0.40F;
        float b = relay.isReceiver ? 1.0F : 0.80F;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(BEAM_TEX));
        Matrix4f posMat = poseStack.last().pose();
        Matrix3f normMat = poseStack.last().normal();

        // 5. Внешний кристалл (радиус 0.48 перекрывает вал толщиной 0.25)
        drawHexCrystal(posMat, normMat, consumer, 0.48F, 0.18F, 0.42F, r, g, b, 0.65F);

        // 6. Внутреннее яркое ядро
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-angle * 1.5F));
        drawHexCrystal(poseStack.last().pose(), poseStack.last().normal(), consumer, 0.26F, 0.10F, 0.24F, 1.0F, 0.85F, 1.0F, 0.85F);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void drawHexCrystal(Matrix4f mat, Matrix3f norm, VertexConsumer b,
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

            // Верхние грани к острию
            addQuad(b, mat, norm,
                    0.0F, tipH, 0.0F,
                    0.0F, tipH, 0.0F,
                    hx[next], beltHalfH, hz[next],
                    hx[i], beltHalfH, hz[i],
                    r, g, bCol, a);

            // Боковой пояс кристалла
            addQuad(b, mat, norm,
                    hx[i], -beltHalfH, hz[i],
                    hx[next], -beltHalfH, hz[next],
                    hx[next], beltHalfH, hz[next],
                    hx[i], beltHalfH, hz[i],
                    r, g, bCol, a);

            // Нижние грани к острию
            addQuad(b, mat, norm,
                    hx[i], -beltHalfH, hz[i],
                    hx[next], -beltHalfH, hz[next],
                    0.0F, -tipH, 0.0F,
                    0.0F, -tipH, 0.0F,
                    r, g, bCol, a);
        }
    }

    private void addQuad(VertexConsumer b, Matrix4f mat, Matrix3f norm,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         float x4, float y4, float z4,
                         float r, float g, float bCol, float a) {
        // Лицевая сторона
        putV(b, mat, norm, x1, y1, z1, 0.0F, 0.0F, r, g, bCol, a);
        putV(b, mat, norm, x2, y2, z2, 1.0F, 0.0F, r, g, bCol, a);
        putV(b, mat, norm, x3, y3, z3, 1.0F, 1.0F, r, g, bCol, a);
        putV(b, mat, norm, x4, y4, z4, 0.0F, 1.0F, r, g, bCol, a);

        // Обратная сторона (для видимости сквозь прозрачные грани)
        putV(b, mat, norm, x4, y4, z4, 0.0F, 1.0F, r, g, bCol, a);
        putV(b, mat, norm, x3, y3, z3, 1.0F, 1.0F, r, g, bCol, a);
        putV(b, mat, norm, x2, y2, z2, 1.0F, 0.0F, r, g, bCol, a);
        putV(b, mat, norm, x1, y1, z1, 0.0F, 0.0F, r, g, bCol, a);
    }

    private void putV(VertexConsumer b, Matrix4f mat, Matrix3f norm,
                      float x, float y, float z,
                      float u, float v,
                      float r, float g, float bCol, float a) {
        b.vertex(mat, x, y, z)
                .color(r, g, bCol, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880) // 0xF000F0 — максимальная яркость
                .normal(norm, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}