package com.example.arcanebridge.client.render;

import com.example.arcanebridge.combat.MobArchetypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import org.joml.Matrix4f;
import top.theillusivec4.curios.api.CuriosApi;

import java.lang.reflect.Field;
import java.util.List;

@Mod.EventBusSubscriber(modid = MobArchetypes.MODID, value = Dist.CLIENT)
public class ShieldBarOverlayRenderer {

    private static final double MAX_RENDER_DISTANCE_SQ = 24.0 * 24.0;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        boolean hasGoggles = hasEngineerGoggles(player);
        boolean hasHudJack = hasCyberware(player, "cyber_ware_port:cybereye_upgrades_hudjack") || hasCyberware(player, "cyber_ware_port:cybereyes");

        if (!hasGoggles && !hasHudJack) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();

        List<Entity> entities = mc.level.getEntities(player, player.getBoundingBox().inflate(24.0),
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
            int remainingLayers = layers.size() - currentIndex;

            if (hasHudJack) {
                renderNeatContourShield(poseStack, cameraPos, target, currentHp, maxHp, typeStr, remainingLayers, event.getPartialTick());
            } else {
                renderGogglesFloatingText(poseStack, cameraPos, target, currentHp, maxHp, typeStr, remainingLayers, event.getPartialTick());
            }
        }
    }

    /**
     * РЕЖИМ 1: Очки Инженера (Чистые парящие цифры над мобом без Neat)
     */
    private static void renderGogglesFloatingText(PoseStack poseStack, Vec3 cameraPos, LivingEntity target,
                                                 float currentHp, float maxHp, String typeStr, int remainingLayers,
                                                 float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        double x = target.xo + (target.getX() - target.xo) * partialTick - cameraPos.x;
        double y = target.yo + (target.getY() - target.yo) * partialTick - cameraPos.y;
        double z = target.zo + (target.getZ() - target.zo) * partialTick - cameraPos.z;

        poseStack.pushPose();
        poseStack.translate(x, y + target.getBbHeight() + 0.35D, z);

        Camera camera = mc.gameRenderer.getMainCamera();
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        poseStack.scale(-0.020F, -0.020F, 0.020F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        Matrix4f mat = poseStack.last().pose();

        String colorCode = switch (typeStr) {
            case "ARMORED" -> "§6";
            case "ETHEREAL" -> "§d";
            case "BIO" -> "§a";
            default -> "§b";
        };

        String stackInfo = remainingLayers > 1 ? " §7x" + remainingLayers : "";
        String text = String.format("%s%.0f§7/§f%.0f%s", colorCode, currentHp, maxHp, stackInfo);

        int textWidth = font.width(text);
        int light = LevelRenderer.getLightColor(target.level(), target.blockPosition());

        font.drawInBatch(text, -textWidth / 2.0F, 0, 0xFFFFFFFF, true, mat,
                mc.renderBuffers().bufferSource(), Font.DisplayMode.NORMAL, 0, light);
        mc.renderBuffers().bufferSource().endBatch();

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    /**
     * РЕЖИМ 2: HUD Jack (Точная динамическая обводка по контуру плашки Neat)
     */
    private static void renderNeatContourShield(PoseStack poseStack, Vec3 cameraPos, LivingEntity target,
                                                float currentHp, float maxHp, String typeStr, int remainingLayers,
                                                float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        double x = target.xo + (target.getX() - target.xo) * partialTick - cameraPos.x;
        double y = target.yo + (target.getY() - target.yo) * partialTick - cameraPos.y;
        double z = target.zo + (target.getZ() - target.zo) * partialTick - cameraPos.z;

        // Позиция Neat: Y = bbHeight + 0.6D[cite: 3]
        double heightOffset = target.getBbHeight() + 0.6D;

        poseStack.pushPose();
        poseStack.translate(x, y + heightOffset, z);

        Camera camera = mc.gameRenderer.getMainCamera();
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        poseStack.scale(-0.02666667F, -0.02666667F, 0.02666667F); // Точный масштаб Neat[cite: 3]

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        Matrix4f mat = poseStack.last().pose();

        int shieldColor;
        String colorCode;
        switch (typeStr) {
            case "ARMORED" -> {
                shieldColor = 0xFFFFD700; // Золотой
                colorCode = "§6";
            }
            case "ETHEREAL" -> {
                shieldColor = 0xFFDDA0DD; // Аметистовый
                colorCode = "§d";
            }
            case "BIO" -> {
                shieldColor = 0xFF55FF55; // Зеленый
                colorCode = "§a";
            }
            default -> {
                shieldColor = 0xFF00FFFF;
                colorCode = "§b";
            }
        }

        // Динамический расчет ширины Neat по формуле мода: halfSize = (int)(maxHealth * plateSize)[cite: 3]
        float targetMaxHealth = target.getMaxHealth();
        int halfSize = (int) Math.max(20, Math.min(48, targetMaxHealth)); //[cite: 3]
        
        // Габариты фона Neat: от X = (-halfSize - 2) до (+halfSize + 2), от Y = -8 (верх имени) до +5 (низ бара)[cite: 3]
        int minX = -halfSize - 2;
        int maxX = halfSize + 2;
        int minY = -9;
        int maxY = 5;
        
        float progress = Math.max(0.0F, Math.min(1.0F, currentHp / maxHp));

        // 1. Отрисовка сгорающей обводки по часовой стрелке
        drawPerimeterShieldFrame(mat, minX, minY, maxX, maxY, progress, shieldColor);

        // 2. Цифры щита строго под плашкой Neat на чистом фоне
        String stackInfo = remainingLayers > 1 ? " §7x" + remainingLayers : "";
        String text = String.format("%s%.0f§7/§f%.0f%s", colorCode, currentHp, maxHp, stackInfo);

        poseStack.pushPose();
        poseStack.translate(0, maxY + 3.0F, 0);
        poseStack.scale(0.55F, 0.55F, 0.55F);

        int textWidth = font.width(text);
        int light = LevelRenderer.getLightColor(target.level(), target.blockPosition());

        font.drawInBatch(
                text,
                -textWidth / 2.0F,
                0,
                0xFFFFFFFF,
                true, // Четкая тень
                mat,
                mc.renderBuffers().bufferSource(),
                Font.DisplayMode.NORMAL,
                0,
                light
        );
        mc.renderBuffers().bufferSource().endBatch();
        poseStack.popPose();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    /**
     * Отрисовка рамки по часовой стрелке: Верх (L->R) -> Право (T->B) -> Низ (R->L) -> Лево (B->T)
     */
    private static void drawPerimeterShieldFrame(Matrix4f matrix, int minX, int minY, int maxX, int maxY, float progress, int color) {
        int width = maxX - minX;
        int height = maxY - minY;
        int totalPerimeter = (width * 2) + (height * 2);
        int remainingLength = (int) (totalPerimeter * progress);

        // 1. Верхняя грань (слева направо)
        int topLen = Math.min(remainingLength, width);
        if (topLen > 0) {
            fill(matrix, minX, minY, minX + topLen, minY + 1, color);
            remainingLength -= topLen;
        }

        // 2. Правая грань (сверху вниз)
        if (remainingLength > 0) {
            int rightLen = Math.min(remainingLength, height);
            fill(matrix, maxX - 1, minY, maxX, minY + rightLen, color);
            remainingLength -= rightLen;
        }

        // 3. Нижняя грань (справа налево)
        if (remainingLength > 0) {
            int botLen = Math.min(remainingLength, width);
            fill(matrix, maxX - botLen, maxY - 1, maxX, maxY, color);
            remainingLength -= botLen;
        }

        // 4. Левая грань (снизу вверх)
        if (remainingLength > 0) {
            int leftLen = Math.min(remainingLength, height);
            fill(matrix, minX, maxY - leftLen, minX + 1, maxY, color);
        }
    }

    private static void fill(Matrix4f matrix, int minX, int minY, int maxX, int maxY, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        var buffer = com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, minX, maxY, 0.0F).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, maxX, maxY, 0.0F).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, maxX, minY, 0.0F).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, minX, minY, 0.0F).color(r, g, b, a).endVertex();
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer.end());
    }

    private static boolean hasEngineerGoggles(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (isGogglesItem(head)) return true;

        try {
            var curiosInventory = CuriosApi.getCuriosInventory(player);
            if (curiosInventory.isPresent()) {
                var handler = curiosInventory.orElse(null);
                if (handler != null && handler.findFirstCurio(ShieldBarOverlayRenderer::isGogglesItem).isPresent()) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        return false;
    }

    private static boolean isGogglesItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return id.equals("create:goggles") || id.contains("goggle");
    }

    private static boolean hasCyberware(Player player, String targetId) {
        try {
            Class<?> providerClass = Class.forName("com.maxwell.cyber_ware_port.common.capability.CyberwareCapabilityProvider");
            Field capField = providerClass.getField("CYBERWARE_CAPABILITY");
            net.minecraftforge.common.capabilities.Capability<?> cap =
                    (net.minecraftforge.common.capabilities.Capability<?>) capField.get(null);

            var lazyOpt = player.getCapability(cap);
            if (lazyOpt.isPresent()) {
                Object userData = lazyOpt.orElse(null);
                if (userData != null) {
                    for (Field field : userData.getClass().getDeclaredFields()) {
                        field.setAccessible(true);
                        Object val = field.get(userData);

                        if (val instanceof IItemHandler handler) {
                            for (int i = 0; i < handler.getSlots(); i++) {
                                ItemStack stack = handler.getStackInSlot(i);
                                if (!stack.isEmpty()) {
                                    String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                                    if (id.equals(targetId)) return true;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }
}