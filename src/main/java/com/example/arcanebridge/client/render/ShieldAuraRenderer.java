package com.example.arcanebridge.client.render;

import com.example.arcanebridge.combat.MobArchetypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.List;

@Mod.EventBusSubscriber(modid = MobArchetypes.MODID, value = Dist.CLIENT)
public class ShieldAuraRenderer {

    private static final double MAX_RENDER_DISTANCE_SQ = 32.0 * 32.0;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        float partialTick = event.getPartialTick();

        List<Entity> entities = mc.level.getEntities(player, player.getBoundingBox().inflate(32.0),
                e -> e instanceof LivingEntity && !(e instanceof Player) && e.isAlive());

        for (Entity ent : entities) {
            LivingEntity target = (LivingEntity) ent;
            if (target.distanceToSqr(player) > MAX_RENDER_DISTANCE_SQ) continue;

            CompoundTag data = target.getPersistentData();
            if (data.getBoolean(MobArchetypes.NBT_ALL_SHIELDS_BROKEN)) continue;
            if (!data.contains(MobArchetypes.NBT_SHIELD_LAYERS, Tag.TAG_LIST)) continue;

            ListTag layers = data.getList(MobArchetypes.NBT_SHIELD_LAYERS, Tag.TAG_COMPOUND);
            int currentIndex = data.getInt(MobArchetypes.NBT_CURRENT_LAYER_INDEX);
            if (layers.isEmpty() || currentIndex >= layers.size()) continue;

            CompoundTag activeLayer = layers.getCompound(currentIndex);
            float currentHp = activeLayer.getFloat("HP");
            float maxHp = activeLayer.getFloat("MaxHP");
            if (maxHp <= 0.0f || currentHp <= 0.0f) continue;

            String typeStr = activeLayer.getString("Type");

            // Цветовой профиль барьера
            float r, g, b;
            switch (typeStr) {
                case "ARMORED" -> {
                    r = 1.00F; g = 0.80F; b = 0.20F; // Золото / латунь
                }
                case "ETHEREAL" -> {
                    r = 0.88F; g = 0.38F; b = 1.00F; // Аметист
                }
                case "BIO" -> {
                    r = 0.25F; g = 0.95F; b = 0.40F; // Био-зеленый
                }
                default -> {
                    r = 0.20F; g = 0.85F; b = 1.00F; // Энергетический лазурный
                }
            }

            renderEntityEnergyCapsule(poseStack, cameraPos, target, r, g, b, partialTick);
        }
    }

    /**
     * Отрисовка высокотехнологичной силовой капсулы Френеля вокруг хитбокса
     */
    private static void renderEntityEnergyCapsule(PoseStack poseStack, Vec3 cameraPos, LivingEntity target,
                                                 float r, float g, float b, float partialTick) {
        double x = Mth.lerp(partialTick, target.xo, target.getX()) - cameraPos.x;
        double y = Mth.lerp(partialTick, target.yo, target.getY()) - cameraPos.y;
        double z = Mth.lerp(partialTick, target.zo, target.getZ()) - cameraPos.z;

        float width = target.getBbWidth();
        float height = target.getBbHeight();

        float time = target.tickCount + partialTick;

        // Плавная пульсация объема поля (дыхание)
        float pulse = 1.0F + (float) Math.sin(time * 0.07F) * 0.035F;

        // Радиус и высота капсулы с учетом отступа от тела
        float radius = (width * 0.55F + 0.18F) * pulse;
        float totalHeight = height + 0.25F;
        float capRadius = Math.min(radius, totalHeight * 0.30F);
        float cylinderHeight = Math.max(0.05F, totalHeight - capRadius * 2.0F);

        // Прозрачность: базовая ~0.20, на краях ~0.55, вспышка при ударе до ~0.75
        float baseAlpha = 0.22F + (float) Math.sin(time * 0.07F) * 0.04F;
        if (target.hurtTime > 0) {
            baseAlpha = Math.min(0.75F, baseAlpha + 0.45F);
            radius *= 1.05F;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        poseStack.pushPose();
        poseStack.translate(x, y - 0.05D, z);

        Matrix4f mat = poseStack.last().pose();

        // 1. Внешняя капсула с эффектом краевого свечения Френеля
        drawCapsuleMesh(mat, radius, cylinderHeight, capRadius, r, g, b, baseAlpha, 18, 24);

        // 2. Внутренний энергетический контур (чуть меньше и мягче)
        drawCapsuleMesh(mat, radius * 0.94F, cylinderHeight * 0.96F, capRadius * 0.94F, r * 1.1F, g * 1.1F, b * 1.1F, baseAlpha * 0.45F, 12, 16);

        poseStack.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /**
     * Генерация 3D-меша силовой капсулы (Нижний купол -> Цилиндр -> Верхний купол)
     */
    private static void drawCapsuleMesh(Matrix4f matrix, float radius, float cylinderH, float capRadius,
                                       float r, float g, float b, float baseAlpha,
                                       int rings, int segments) {
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        int totalVerticalSteps = rings * 2 + 1;

        for (int v = 0; v < totalVerticalSteps; v++) {
            float t1 = (float) v / totalVerticalSteps;
            float t2 = (float) (v + 1) / totalVerticalSteps;

            float y1 = calculateCapsuleY(t1, cylinderH, capRadius);
            float rad1 = calculateCapsuleRadius(t1, radius);

            float y2 = calculateCapsuleY(t2, cylinderH, capRadius);
            float rad2 = calculateCapsuleRadius(t2, radius);

            // Краевой градиент альфа-канала (эффект Френеля)
            float edgeFactor1 = (float) Math.sin(t1 * Math.PI);
            float edgeFactor2 = (float) Math.sin(t2 * Math.PI);

            float a1 = baseAlpha * (0.35F + 0.65F * edgeFactor1);
            float a2 = baseAlpha * (0.35F + 0.65F * edgeFactor2);

            for (int s = 0; s < segments; s++) {
                float phi1 = (float) (s * 2.0 * Math.PI / segments);
                float phi2 = (float) ((s + 1) * 2.0 * Math.PI / segments);

                float x1a = (float) Math.cos(phi1) * rad1;
                float z1a = (float) Math.sin(phi1) * rad1;

                float x2a = (float) Math.cos(phi2) * rad1;
                float z2a = (float) Math.sin(phi2) * rad1;

                float x1b = (float) Math.cos(phi1) * rad2;
                float z1b = (float) Math.sin(phi1) * rad2;

                float x2b = (float) Math.cos(phi2) * rad2;
                float z2b = (float) Math.sin(phi2) * rad2;

                buffer.vertex(matrix, x1a, y1, z1a).color(r, g, b, a1).endVertex();
                buffer.vertex(matrix, x2a, y1, z2a).color(r, g, b, a1).endVertex();
                buffer.vertex(matrix, x2b, y2, z2b).color(r, g, b, a2).endVertex();
                buffer.vertex(matrix, x1b, y2, z1b).color(r, g, b, a2).endVertex();
            }
        }

        BufferUploader.drawWithShader(buffer.end());
    }

    private static float calculateCapsuleY(float t, float cylinderH, float capRadius) {
        if (t <= 0.25F) {
            // Нижний полусферический купол
            float angle = (t / 0.25F) * (Mth.PI / 2.0F);
            return capRadius * (1.0F - (float) Math.cos(angle));
        } else if (t <= 0.75F) {
            // Цилиндрическая середина тела
            float norm = (t - 0.25F) / 0.50F;
            return capRadius + norm * cylinderH;
        } else {
            // Верхний полусферический купол
            float angle = ((t - 0.75F) / 0.25F) * (Mth.PI / 2.0F);
            return capRadius + cylinderH + capRadius * (float) Math.sin(angle);
        }
    }

    private static float calculateCapsuleRadius(float t, float maxRadius) {
        if (t <= 0.25F) {
            float angle = (t / 0.25F) * (Mth.PI / 2.0F);
            return maxRadius * (float) Math.sin(angle);
        } else if (t <= 0.75F) {
            return maxRadius;
        } else {
            float angle = ((t - 0.75F) / 0.25F) * (Mth.PI / 2.0F);
            return maxRadius * (float) Math.cos(angle);
        }
    }
}