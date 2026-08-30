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

            renderShieldBar(poseStack, cameraPos, target, currentHp, maxHp, typeStr, remainingLayers, event.getPartialTick());
        }
    }

    private static void renderShieldBar(PoseStack poseStack, Vec3 cameraPos, LivingEntity target,
                                        float currentHp, float maxHp, String typeStr, int remainingLayers,
                                        float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        double x = target.xo + (target.getX() - target.xo) * partialTick - cameraPos.x;
        double y = target.yo + (target.getY() - target.yo) * partialTick - cameraPos.y;
        double z = target.zo + (target.getZ() - target.zo) * partialTick - cameraPos.z;

        // Точное позиционирование на уровне плашки Neat над головой
        double heightOffset = target.getBbHeight() + 0.52D;

        poseStack.pushPose();
        poseStack.translate(x, y + heightOffset, z);

        Camera camera = mc.gameRenderer.getMainCamera();
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        poseStack.scale(-0.020F, -0.020F, 0.020F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        Matrix4f mat = poseStack.last().pose();

        // Мягкая невыбивающаяся палитра
        int barColor;
        String icon;
        switch (typeStr) {
            case "ARMORED" -> {
                barColor = 0xFFD49B2A; // Латунь Create
                icon = "⚙";
            }
            case "ETHEREAL" -> {
                barColor = 0xFF8E44AD; // Спокойный аметист
                icon = "🔮";
            }
            case "BIO" -> {
                barColor = 0xFF27AE60; // Насыщенный био-зеленый
                icon = "🧬";
            }
            default -> {
                barColor = 0xFF2980B9;
                icon = "🛡";
            }
        }

        int totalWidth = 36;
        int barHeight = 2;
        int halfWidth = totalWidth / 2;

        // 1. Тонкая подложка накладки (Neat Style)
        fill(mat, -halfWidth, 0, halfWidth, barHeight, 0x99111111);

        // 2. Активная полоса барьера
        float progress = Math.max(0.0F, Math.min(1.0F, currentHp / maxHp));
        int filledWidth = (int) (totalWidth * progress);
        if (filledWidth > 0) {
            fill(mat, -halfWidth, 0, -halfWidth + filledWidth, barHeight, barColor);
        }

        // 3. Компактный индикатор справа от полосы
        String stackInfo = remainingLayers > 1 ? "x" + remainingLayers : "";
        String badge = String.format("%s%.0f %s", icon, currentHp, stackInfo).trim();

        poseStack.pushPose();
        poseStack.translate(halfWidth + 3, -2.0F, 0);
        poseStack.scale(0.65F, 0.65F, 0.65F);

        // Читаем реальный свет моба в мире
        int light = LevelRenderer.getLightColor(target.level(), target.blockPosition());
        font.drawInBatch(badge, 0, 0, 0xFFE0E0E0, false, mat,
                mc.renderBuffers().bufferSource(), Font.DisplayMode.NORMAL, 0, light);
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