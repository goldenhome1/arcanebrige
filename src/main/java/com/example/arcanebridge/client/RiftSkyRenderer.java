package com.example.arcanebridge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "arcane_bridge", value = Dist.CLIENT)
public class RiftSkyRenderer {

    private static final ResourceLocation TEX_VORTEX_CLOUD = new ResourceLocation("arcane_bridge", "textures/vfx/vortex_cloud.png");
    private static final ResourceLocation TEX_VORTEX_CORE  = new ResourceLocation("arcane_bridge", "textures/vfx/vortex_core.png");
    private static final ResourceLocation TEX_RIFT_ARCANE  = new ResourceLocation("arcane_bridge", "textures/vfx/sky_rift_arcane.png");
    private static final ResourceLocation TEX_RIFT_SYND    = new ResourceLocation("arcane_bridge", "textures/vfx/sky_rift_syndicate.png");
    private static final ResourceLocation TEX_RIFT_THERMAL = new ResourceLocation("arcane_bridge", "textures/vfx/sky_rift_thermal.png");
    private static final ResourceLocation TEX_FRACTURE     = new ResourceLocation("arcane_bridge", "textures/vfx/sky_fracture_vertical.png");

    public static boolean isPrepActive = false;
    public static int prepTimerSeconds = 0;
    public static int maxPrepSeconds = 120;
    public static boolean isRaidActive = false;
    public static String currentRaidType = "arcane_breach";
    public static UUID targetUUID = new UUID(0L, 0L);
    public static BlockPos raidEpicenter = BlockPos.ZERO;

    private static float rotationTick = 0.0F;
    private static float smoothPrepSeconds = 0.0F;
    private static float openProgress = 0.0F;
    private static float flashIntensity = 0.0F;
    private static boolean prevRaidState = false;

    // Параметры: {Yaw (0-360°), Pitch (-5° до 65°), Базовая ширина, Высота, Фаза времени, Скорость цикла}
    private static final float[][] FRACTURE_SLOTS = new float[][]{
            // --- ГОРИЗОНТ (Прямо перед глазами и чуть ниже/выше линии земли) ---
            { 18.0F,   2.0F,  16.0F, 75.0F, 0.0F, 0.045F },
            { 68.0F,  -3.0F,  18.0F, 85.0F, 1.8F, 0.060F },
            { 125.0F,  5.0F,  15.0F, 70.0F, 3.4F, 0.038F },
            { 185.0F, -2.0F,  20.0F, 90.0F, 5.1F, 0.052F },
            { 245.0F,  4.0F,  17.0F, 80.0F, 2.3F, 0.042F },
            { 305.0F, -4.0F,  22.0F, 95.0F, 4.0F, 0.058F },

            // --- СРЕДНЯЯ ВЫСОТА НЕБА ---
            { 42.0F,  26.0F,  15.0F, 60.0F, 1.1F, 0.050F },
            { 95.0F,  34.0F,  18.0F, 68.0F, 2.9F, 0.044F },
            { 160.0F, 22.0F,  14.0F, 55.0F, 4.6F, 0.062F },
            { 220.0F, 30.0F,  19.0F, 72.0F, 0.7F, 0.040F },
            { 280.0F, 28.0F,  16.0F, 62.0F, 3.8F, 0.055F },
            { 340.0F, 35.0F,  17.0F, 65.0F, 5.7F, 0.048F },

            // --- ВЫСОКОЕ НЕБО (Подступы к разлому) ---
            { 55.0F,  52.0F,  14.0F, 50.0F, 2.0F, 0.065F },
            { 145.0F, 58.0F,  16.0F, 56.0F, 4.2F, 0.047F },
            { 235.0F, 48.0F,  15.0F, 52.0F, 1.5F, 0.053F },
            { 315.0F, 55.0F,  18.0F, 58.0F, 3.1F, 0.041F }
    };

    public static void updateRaidState(boolean prep, int prepSec, int maxSec, boolean raid, String type, UUID target, BlockPos epicenter) {
        if (raid && !prevRaidState) {
            flashIntensity = 1.0F;
        }

        isPrepActive = prep;
        prepTimerSeconds = prepSec;
        maxPrepSeconds = Math.max(1, maxSec);
        isRaidActive = raid;
        prevRaidState = raid;
        currentRaidType = type != null ? type : "arcane_breach";
        targetUUID = target;
        raidEpicenter = epicenter;

        if (isPrepActive && Math.abs(smoothPrepSeconds - prepSec) > 2.0F) {
            smoothPrepSeconds = prepSec;
        }

        if (!isPrepActive && !isRaidActive) {
            openProgress = 0.0F;
            smoothPrepSeconds = 0.0F;
            flashIntensity = 0.0F;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        rotationTick += 1.0F;

        if (flashIntensity > 0.001F) {
            flashIntensity = Math.max(0.0F, flashIntensity - 0.04F);
        }

        if (isPrepActive) {
            smoothPrepSeconds = Math.max(0.0F, smoothPrepSeconds - 0.05F);
            openProgress = 0.0F;

            if (prepTimerSeconds <= 1 && flashIntensity < 0.2F) {
                flashIntensity = 0.85F;
            }
        } else if (isRaidActive) {
            openProgress = Math.min(1.0F, openProgress + 0.10F);
        } else {
            openProgress = Math.max(0.0F, openProgress - 0.04F);
        }
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!isPrepActive && !isRaidActive && flashIntensity <= 0.001F) return;

        float intensity = isRaidActive ? 0.60F : Math.max(0.05F, 1.0F - (smoothPrepSeconds / (float) maxPrepSeconds)) * 0.50F;

        float tr = 0.18F, tg = 0.04F, tb = 0.28F;
        if ("syndicate_raid".equalsIgnoreCase(currentRaidType)) {
            tr = 0.28F; tg = 0.14F; tb = 0.03F;
        } else if ("thermal_surge".equalsIgnoreCase(currentRaidType)) {
            tr = 0.35F; tg = 0.06F; tb = 0.02F;
        }

        float r = event.getRed() * (1.0F - intensity) + tr * intensity;
        float g = event.getGreen() * (1.0F - intensity) + tg * intensity;
        float b = event.getBlue() * (1.0F - intensity) + tb * intensity;

        if (flashIntensity > 0.0F) {
            r = r * (1.0F - flashIntensity) + 1.0F * flashIntensity;
            g = g * (1.0F - flashIntensity) + 0.95F * flashIntensity;
            b = b * (1.0F - flashIntensity) + 1.0F * flashIntensity;
        }

        event.setRed(r);
        event.setGreen(g);
        event.setBlue(b);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        if (!isPrepActive && !isRaidActive && openProgress <= 0.001F && flashIntensity <= 0.001F) return;

        Minecraft mc = Minecraft.getInstance();
        Player localPlayer = mc.player;
        if (localPlayer == null || mc.level == null) return;

        PoseStack poseStack = event.getPoseStack();
        float pTick = event.getPartialTick();
        float curRot = rotationTick + pTick;

        // Базовые цветовые профили
        float baseR = 0.85F, baseG = 0.30F, baseB = 1.0F;
        ResourceLocation riftTexture = TEX_RIFT_ARCANE;
        if ("syndicate_raid".equalsIgnoreCase(currentRaidType)) {
            baseR = 1.0F; baseG = 0.65F; baseB = 0.15F;
            riftTexture = TEX_RIFT_SYND;
        } else if ("thermal_surge".equalsIgnoreCase(currentRaidType)) {
            baseR = 1.0F; baseG = 0.30F; baseB = 0.05F;
            riftTexture = TEX_RIFT_THERMAL;
        }

        float prepProgress = isPrepActive ? (1.0F - (smoothPrepSeconds / (float) maxPrepSeconds)) : 1.0F;
        float vortexAlphaFactor = Math.max(0.0F, 1.0F - openProgress * 1.5F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        poseStack.pushPose();

        // ----------------------------------------------------
        // ⚡ 1. Ослепляющая вспышка молнии
        // ----------------------------------------------------
        if (flashIntensity > 0.01F) {
            drawFullSkyPlane(poseStack.last().pose(), TEX_VORTEX_CORE, 140.0F, 1.0F, 0.95F, 1.0F, flashIntensity * 0.9F);
        }

        // ----------------------------------------------------
        // 🌀 2. Облачная воронка (Цельная, плавная)
        // ----------------------------------------------------
        if (vortexAlphaFactor > 0.01F) {
            float collapseScale = 36.0F * (1.0F - prepProgress * 0.35F);

            // Внешние тучи
            float cloudAlpha = (0.05F + prepProgress * 0.55F) * vortexAlphaFactor;
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(curRot * 0.35F));
            drawFullSkyPlane(poseStack.last().pose(), TEX_VORTEX_CLOUD, collapseScale, 0.15F, 0.15F, 0.18F, cloudAlpha);
            poseStack.popPose();

            // Внутренний вихрь
            if (prepProgress > 0.25F) {
                float innerAlpha = ((prepProgress - 0.25F) / 0.75F * 0.75F) * vortexAlphaFactor;
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(-curRot * 0.85F));
                drawFullSkyPlane(poseStack.last().pose(), TEX_VORTEX_CLOUD, collapseScale * 0.75F, baseR * 0.8F, baseG * 0.8F, baseB * 0.8F, innerAlpha);
                poseStack.popPose();
            }

            // Ядро
            if (prepProgress > 0.45F) {
                float coreAlpha = ((prepProgress - 0.45F) / 0.55F * 0.85F) * vortexAlphaFactor;
                float coreScale = (collapseScale * 0.4F) + (float) Math.sin(curRot * 0.2F) * 1.2F;
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(curRot * 1.6F));
                drawFullSkyPlane(poseStack.last().pose(), TEX_VORTEX_CORE, coreScale, baseR, baseG, baseB, coreAlpha);
                poseStack.popPose();
            }
        }

        // ----------------------------------------------------
        // 🌌 3. Основной Разлом в зените
        // ----------------------------------------------------
        if (openProgress > 0.01F) {
            float riftScale = 220.0F * openProgress;
            float plasmaWave = (float) Math.pow(Math.max(0.0F, Math.sin(curRot * 0.06F)), 3.0);
            boolean lightningFlicker = ((int)(curRot) % 35 < 2) || ((int)(curRot) % 80 < 3);

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-15.0F));

            // Базовый силуэт
            drawFullSkyPlane(poseStack.last().pose(), riftTexture, riftScale, 0.06F, 0.04F, 0.08F, openProgress * 0.95F);

            // Плазма
            float plasmaBright = 0.25F + plasmaWave * 0.85F;
            drawFullSkyPlane(poseStack.last().pose(), riftTexture, riftScale,
                    baseR * plasmaBright, baseG * plasmaBright, baseB * plasmaBright, openProgress * 0.85F);

            // Молнии
            if (lightningFlicker) {
                drawFullSkyPlane(poseStack.last().pose(), riftTexture, riftScale, 1.0F, 0.98F, 1.0F, 0.95F);
            }

            poseStack.popPose();

            // ----------------------------------------------------
            // ⚡ 4. Динамические мерцающие трещины по всему куполу
            // ----------------------------------------------------
            for (float[] conf : FRACTURE_SLOTS) {
                float yaw = conf[0];
                float pitch = conf[1];
                float baseW = conf[2];
                float baseH = conf[3];
                float phase = conf[4];
                float speed = conf[5];

                // 1. Цикл жизни: появление -> пик свечения -> затухание -> исчезновение
                float cycle = (float) Math.sin(curRot * speed + phase);
                if (cycle <= 0.15F) continue; // Трещина невидима

                float lifeProgress = (cycle - 0.15F) / 0.85F; // 0.0 -> 1.0 -> 0.0
                float alpha = (float) Math.pow(lifeProgress, 1.5) * openProgress;

                // 2. Динамическое расширение по ширине (растяжение материи)
                float widthExpand = 1.0F + (float) Math.sin(curRot * 0.12F + phase) * 0.35F;
                float currentWidth = baseW * widthExpand;

                // 3. Электрические микро-всплески яркости
                boolean microFlash = ((int)(curRot + phase * 20.0F) % 28 < 2);
                float bright = 0.30F + lifeProgress * 0.70F + (microFlash ? 0.65F : 0.0F);

                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
                poseStack.mulPose(Axis.XP.rotationDegrees(-pitch)); // Минусовой pitch для корректного наклона

                drawHorizonFacingFracture(poseStack.last().pose(), TEX_FRACTURE, currentWidth, baseH,
                        baseR * bright, baseG * bright, baseB * bright, alpha);

                poseStack.popPose();
            }
        }

        poseStack.popPose();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void drawFullSkyPlane(Matrix4f matrix4f, ResourceLocation texture, float size,
                                         float r, float g, float b, float a) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(
                Math.min(1.0F, Math.max(0.0F, r)),
                Math.min(1.0F, Math.max(0.0F, g)),
                Math.min(1.0F, Math.max(0.0F, b)),
                Math.min(1.0F, Math.max(0.0F, a))
        );

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        float height = 100.0F;
        buffer.vertex(matrix4f, -size, height, -size).uv(0.0F, 0.0F).endVertex();
        buffer.vertex(matrix4f, -size, height,  size).uv(0.0F, 1.0F).endVertex();
        buffer.vertex(matrix4f,  size, height,  size).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(matrix4f,  size, height, -size).uv(1.0F, 0.0F).endVertex();

        BufferUploader.drawWithShader(buffer.end());
    }

    private static void drawHorizonFacingFracture(Matrix4f matrix4f, ResourceLocation texture,
                                                  float halfW, float halfH,
                                                  float r, float g, float b, float a) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(
                Math.min(1.0F, Math.max(0.0F, r)),
                Math.min(1.0F, Math.max(0.0F, g)),
                Math.min(1.0F, Math.max(0.0F, b)),
                Math.min(1.0F, Math.max(0.0F, a))
        );

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        float dist = 95.0F;
        buffer.vertex(matrix4f, -halfW, -halfH, dist).uv(0.0F, 1.0F).endVertex();
        buffer.vertex(matrix4f,  halfW, -halfH, dist).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(matrix4f,  halfW,  halfH, dist).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(matrix4f, -halfW,  halfH, dist).uv(0.0F, 0.0F).endVertex();

        BufferUploader.drawWithShader(buffer.end());
    }
}