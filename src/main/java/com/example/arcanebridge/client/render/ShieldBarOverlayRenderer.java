package com.example.arcanebridge.client.render;

import com.example.arcanebridge.combat.MobArchetypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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

        // Не рендерим, если у игрока нет очков и нет импланта
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

            renderShieldBar(poseStack, cameraPos, target, currentHp, maxHp, typeStr, remainingLayers, hasHudJack, event.getPartialTick());
        }
    }

    private static void renderShieldBar(PoseStack poseStack, Vec3 cameraPos, LivingEntity target,
                                        float currentHp, float maxHp, String typeStr, int remainingLayers,
                                        boolean hasHudJack, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        double x = target.xo + (target.getX() - target.xo) * partialTick - cameraPos.x;
        double y = target.yo + (target.getY() - target.yo) * partialTick - cameraPos.y;
        double z = target.zo + (target.getZ() - target.zo) * partialTick - cameraPos.z;

        double heightOffset = target.getBbHeight() + (hasHudJack ? 0.65D : 0.30D);

        poseStack.pushPose();
        poseStack.translate(x, y + heightOffset, z);

        Camera camera = mc.gameRenderer.getMainCamera();
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        poseStack.scale(-0.022F, -0.022F, 0.022F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        Matrix4f mat = poseStack.last().pose();

        int barColor;
        String icon;
        switch (typeStr) {
            case "ARMORED" -> {
                barColor = 0xFFFFAA00;
                icon = "⚙";
            }
            case "ETHEREAL" -> {
                barColor = 0xFFAA00FF;
                icon = "🔮";
            }
            case "BIO" -> {
                barColor = 0xFF55FF55;
                icon = "🧬";
            }
            default -> {
                barColor = 0xFF00AAFF;
                icon = "🛡";
            }
        }

        int totalWidth = 44;
        int barHeight = 4;
        int halfWidth = totalWidth / 2;

        fill(mat, -halfWidth - 1, -2, halfWidth + 1, barHeight + 1, 0x88000000);
        fill(mat, -halfWidth - 1, -3, halfWidth + 1, -2, 0xAA333333);
        fill(mat, -halfWidth - 1, barHeight + 1, halfWidth + 1, barHeight + 2, 0xAA333333);
        fill(mat, -halfWidth - 2, -3, -halfWidth - 1, barHeight + 2, 0xAA333333);
        fill(mat, halfWidth + 1, -3, halfWidth + 2, barHeight + 2, 0xAA333333);

        float progress = Math.max(0.0F, Math.min(1.0F, currentHp / maxHp));
        int filledWidth = (int) (totalWidth * progress);
        if (filledWidth > 0) {
            fill(mat, -halfWidth, -1, -halfWidth + filledWidth, barHeight, barColor);
        }

        String stackInfo = remainingLayers > 1 ? " x" + remainingLayers : "";
        String text = String.format("%s %.0f/%.0f%s", icon, currentHp, maxHp, stackInfo);

        poseStack.pushPose();
        poseStack.translate(0, -9.0F, 0);
        poseStack.scale(0.75F, 0.75F, 0.75F);
        int textWidth = font.width(text);
        font.drawInBatch(text, -textWidth / 2.0F, 0, 0xFFFFFFFF, true, mat,
                mc.renderBuffers().bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);
        mc.renderBuffers().bufferSource().endBatch();
        poseStack.popPose();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        poseStack.popPose();
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
        // 1. Проверка ванильного слота головы
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (isGogglesItem(head)) return true;

        // 2. Проверка слотов Curios
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