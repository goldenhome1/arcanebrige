package com.example.arcanebridge.client.render;

import com.example.arcanebridge.entity.ArcaneGuideEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class ShieldDomeLayer extends GeoRenderLayer<ArcaneGuideEntity> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("arcane_bridge", "textures/entity/arcane_guide.png");
    private static final float MAX_RADIUS = 6.5F;

    private static final float UV_CENTER_U = 175.0F / 256.0F;
    private static final float UV_CENTER_V = 75.0F / 256.0F;
    private static final float UV_RADIUS   = 63.0F / 256.0F;

    public ShieldDomeLayer(GeoRenderer<ArcaneGuideEntity> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    public void render(PoseStack poseStack, ArcaneGuideEntity animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {

        int animState = animatable.getAnimState();


        // 1. Отрисовка лазерной плоскости сканирования при материализации/дематериализации

        if (animState == ArcaneGuideEntity.STATE_MATERIALIZE || animState == ArcaneGuideEntity.STATE_DEMATERIALIZE) {

            int totalDuration = (animState == ArcaneGuideEntity.STATE_MATERIALIZE) ? 30 : 25;

            int remaining = animatable.getActionTicks();

            float current = (totalDuration - remaining) + partialTick;

            float rawProgress = org.joml.Math.clamp(current / (float) totalDuration, 0.0F, 1.0F);

            float progress = (animState == ArcaneGuideEntity.STATE_MATERIALIZE) ? rawProgress : (1.0F - rawProgress);


            float scanY = progress * 1.95F; // Движение линии сканирования от 0 до 1.95 блока

            float time = animatable.tickCount + partialTick;


            RenderSystem.enableBlend();

            RenderSystem.defaultBlendFunc();

            RenderSystem.disableCull();

            RenderSystem.depthMask(false);

            RenderSystem.setShader(GameRenderer::getPositionColorShader);


            poseStack.pushPose();

            poseStack.translate(0, scanY, 0);

            drawHologramScanPlane(poseStack.last().pose(), 0.75F, time);

            poseStack.popPose();


            RenderSystem.enableCull();

            RenderSystem.depthMask(true);

            RenderSystem.disableBlend();

            return;

        }


        if (animState != ArcaneGuideEntity.STATE_SHIELD_NIGHT) {

            return;

        }


        float time = animatable.tickCount + partialTick;

        // Плавное раскрытие сферы за первые 3 секунды (60 тиков) с субтиковой интерполяцией
        float currentTicks = animatable.getNightShieldTicks() < 60 ? animatable.getNightShieldTicks() + partialTick : 60.0F;
        float expandProgress = Math.min(1.0F, currentTicks / 60.0F);
        float smoothExpand = (float) Math.sin(expandProgress * Math.PI / 2.0); // EaseOutSine

        float currentRadius = MAX_RADIUS * smoothExpand;
        float pulseAlpha = (0.28F + (float) Math.sin(time * 0.08F) * 0.06F) * smoothExpand;

        if (currentRadius <= 0.05F) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

                        // Вычисляем точное смещение до уровня земли (1.0 блок парения + микрозазор от z-fighting)
        float hoverOffset = 1.0F * smoothExpand;

        // 1. Напольные концентрические магические печати
        poseStack.pushPose();
        poseStack.translate(0, 0.02D - hoverOffset, 0);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);

        // Внешнее руническое кольцо пола
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 0.35F));
        drawRadialGroundSeal(poseStack.last().pose(), currentRadius, 0.95F, 0.35F, 1.0F, 0.90F * smoothExpand, 32);
        poseStack.popPose();

        // Внутреннее руническое кольцо (противоход)
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-time * 0.60F));
        drawRadialGroundSeal(poseStack.last().pose(), currentRadius * 0.48F, 0.40F, 0.75F, 1.0F, 0.85F * smoothExpand, 24);
        poseStack.popPose();

        poseStack.popPose();

        // 2. Полная 3D-сфера поля (центрирована ровно на самом мобе)
        poseStack.pushPose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // Внешняя фиолетовая сфера с мягким свечением горизонта
        drawFresnelEnergySphere(poseStack.last().pose(), currentRadius, 0.78F, 0.22F, 1.0F, pulseAlpha, 24, 32);

        // Внутреннее лазурное силовое ядро
        drawFresnelEnergySphere(poseStack.last().pose(), currentRadius * 0.97F, 0.20F, 0.75F, 1.0F, pulseAlpha * 0.45F, 18, 24);

        poseStack.popPose();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

                private static void drawRadialGroundSeal(Matrix4f matrix4f, float radius, float r, float g, float b, float a, int segments) {
                    RenderSystem.setShaderColor(r, g, b, a);
                    BufferBuilder buffer = Tesselator.getInstance().getBuilder();
                    buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_TEX);

                    buffer.vertex(matrix4f, 0.0F, 0.0F, 0.0F).uv(UV_CENTER_U, UV_CENTER_V).endVertex();

                    for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2.0 * Math.PI / segments);
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            float u = UV_CENTER_U + (float) Math.cos(angle) * UV_RADIUS;
            float v = UV_CENTER_V + (float) Math.sin(angle) * UV_RADIUS;

            buffer.vertex(matrix4f, x, 0.0F, z).uv(u, v).endVertex();
                    }

                    BufferUploader.drawWithShader(buffer.end());
                }

                    private static void drawHologramScanPlane(Matrix4f matrix4f, float radius, float time) {
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // Лазурно-фиолетовая светящаяся линия среза (как на скетче)
        float r1 = 0.20F, g1 = 0.85F, b1 = 1.00F, a1 = 0.85F; // Циан
        float r2 = 1.00F, g2 = 0.20F, b2 = 0.50F, a2 = 0.85F; // Маджента

        buffer.vertex(matrix4f, -radius, 0.0F, -radius).color(r1, g1, b1, a1).endVertex();
        buffer.vertex(matrix4f, -radius, 0.0F,  radius).color(r2, g2, b2, a2).endVertex();
        buffer.vertex(matrix4f,  radius, 0.0F,  radius).color(r1, g1, b1, a1).endVertex();
        buffer.vertex(matrix4f,  radius, 0.0F, -radius).color(r2, g2, b2, a2).endVertex();

        BufferUploader.drawWithShader(buffer.end());
    }

    private static void drawFresnelEnergySphere(Matrix4f matrix4f, float radius,
                                                float r, float g, float b, float baseAlpha,
                                                int rings, int segments) {
                    BufferBuilder buffer = Tesselator.getInstance().getBuilder();
                    buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

                    for (int ring = 0; ring < rings; ring++) {
            float theta1 = (float) (ring * Math.PI / rings);
            float theta2 = (float) ((ring + 1) * Math.PI / rings);

            float y1 = (float) Math.cos(theta1) * radius;
            float r1 = (float) Math.sin(theta1) * radius;

            float y2 = (float) Math.cos(theta2) * radius;
            float r2 = (float) Math.sin(theta2) * radius;

            float alpha1 = baseAlpha * (0.15F + 0.85F * (float) Math.sin(theta1));
            float alpha2 = baseAlpha * (0.15F + 0.85F * (float) Math.sin(theta2));

            for (int seg = 0; seg < segments; seg++) {
                float phi1 = (float) (seg * 2.0 * Math.PI / segments);
                float phi2 = (float) ((seg + 1) * 2.0 * Math.PI / segments);

                float x1 = (float) Math.cos(phi1) * r1;
                float z1 = (float) Math.sin(phi1) * r1;

                float x2 = (float) Math.cos(phi2) * r1;
                float z2 = (float) Math.sin(phi2) * r1;

                float x3 = (float) Math.cos(phi2) * r2;
                float z3 = (float) Math.sin(phi2) * r2;

                float x4 = (float) Math.cos(phi1) * r2;
                float z4 = (float) Math.sin(phi1) * r2;

                buffer.vertex(matrix4f, x1, y1, z1).color(r, g, b, alpha1).endVertex();
                buffer.vertex(matrix4f, x2, y1, z2).color(r, g, b, alpha1).endVertex();
                buffer.vertex(matrix4f, x3, y2, z3).color(r, g, b, alpha2).endVertex();
                buffer.vertex(matrix4f, x4, y2, z4).color(r, g, b, alpha2).endVertex();
            }
                    }

                    BufferUploader.drawWithShader(buffer.end());
                }
}