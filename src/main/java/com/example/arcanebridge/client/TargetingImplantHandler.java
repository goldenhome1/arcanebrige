package com.example.arcanebridge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "arcane_bridge", value = Dist.CLIENT)
public class TargetingImplantHandler {

    private static final List<String> BLOCKED_TRAJECTORY_OVERLAYS = List.of(
            "trajectory",
            "trajectories",
            "crosshair_targeting",
            "ballistics"
    );

    private record WeaponProfile(double speed, double gravity, double drag, double forwardOffset, double motionInheritance) {
        public static final WeaponProfile NONE = new WeaponProfile(0, 0, 0, 0, 0.0);
        public static final WeaponProfile CGS_GUN = new WeaponProfile(8.0, 0.002, 0.999, 0.4, 0.0);
        public static final WeaponProfile CBC_CANNON = new WeaponProfile(5.5, 0.012, 0.995, 2.8, 0.0);
        public static final WeaponProfile CROSSBOW = new WeaponProfile(3.15, 0.05, 0.99, 0.3, 0.5);
    }

    /**
     * 1. Блокировка 2D-оверлеев сторонних модов без связки "Рука + Киберглаза"
     */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        String overlayId = event.getOverlay().id().toString().toLowerCase();

        for (String keyword : BLOCKED_TRAJECTORY_OVERLAYS) {
            if (overlayId.contains(keyword)) {
                Player player = Minecraft.getInstance().player;
                if (player != null && !isFullTargetingSystemActive(player)) {
                    event.setCanceled(true);
                }
                break;
            }
        }
    }

    /**
     * 2. Отрисовка маркера попадания
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !isFullTargetingSystemActive(player)) return;

        WeaponProfile profile = getActiveWeaponProfile(player);
        if (profile != WeaponProfile.NONE) {
            renderTargetMarker(event.getPoseStack(), mc, player, event.getPartialTick(), profile);
        }
    }

    /**
     * Определение активного оружия и параметров баллистики
     */
    private static WeaponProfile getActiveWeaponProfile(Player player) {
        if (player.getVehicle() != null) {
            return WeaponProfile.CBC_CANNON;
        }

        ItemStack mainItem = player.getMainHandItem();
        if (mainItem.isEmpty()) return WeaponProfile.NONE;

        String itemId = BuiltInRegistries.ITEM.getKey(mainItem.getItem()).toString();

        if (itemId.startsWith("cgs:")) {
            return WeaponProfile.CGS_GUN;
        }

        if (mainItem.getItem() instanceof BowItem) {
            if (player.isUsingItem()) {
                int useTicks = player.getTicksUsingItem();
                float pullPower = BowItem.getPowerForTime(useTicks);
                if (pullPower <= 0.05F) return WeaponProfile.NONE;
                return new WeaponProfile(pullPower * 3.0D, 0.05D, 0.99D, 0.3D, 1.0D);
            }
        } else if (mainItem.getItem() instanceof CrossbowItem) {
            if (CrossbowItem.isCharged(mainItem) || player.isUsingItem()) {
                return WeaponProfile.CROSSBOW;
            }
        }

        return WeaponProfile.NONE;
    }

    /**
     * Трассировка баллистики
     */
    private static void renderTargetMarker(PoseStack poseStack, Minecraft mc, Player player, float partialTick, WeaponProfile profile) {
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        Vec3 look;
        Vec3 startPos;

        if (mc.options.getCameraType().isFirstPerson()) {
            Vector3f lookVec = camera.getLookVector();
            look = new Vec3(lookVec.x(), lookVec.y(), lookVec.z());
            startPos = cameraPos.add(look.scale(profile.forwardOffset));
        } else {
            look = player.getViewVector(partialTick);
            startPos = player.getEyePosition(partialTick).add(look.scale(profile.forwardOffset));
        }

        Vec3 velocity = look.scale(profile.speed);

        if (profile.motionInheritance > 0.0) {
            Vec3 playerMotion = player.getDeltaMovement().scale(profile.motionInheritance);
            velocity = velocity.add(playerMotion.x, player.onGround() ? 0.0 : playerMotion.y, playerMotion.z);
        }

        double gravity = profile.gravity;
        double drag = profile.drag;

        Vec3 currentPos = startPos;
        Vec3 hitPoint = null;
        boolean hitEntity = false;

        Entity vehicle = player.getVehicle();

        for (int i = 0; i < 70; i++) {
            Vec3 nextPos = currentPos.add(velocity);

            // Коллизия с блоками
            BlockHitResult blockHit = player.level().clip(new ClipContext(
                    currentPos, nextPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            ));

            Vec3 stepTarget = nextPos;
            if (blockHit.getType() != HitResult.Type.MISS) {
                stepTarget = blockHit.getLocation();
                hitPoint = stepTarget;
            }

            // Коллизия с сущностями
            AABB stepBox = new AABB(currentPos, stepTarget).inflate(0.25);
            List<Entity> entities = player.level().getEntities(
                    player,
                    stepBox,
                    e -> !e.isSpectator() && e.isPickable() && e != vehicle
            );

            for (Entity target : entities) {
                AABB targetBox = target.getBoundingBox().inflate(0.15);
                var clipOpt = targetBox.clip(currentPos, stepTarget);
                if (clipOpt.isPresent()) {
                    hitPoint = clipOpt.get();
                    hitEntity = true;
                    break;
                }
            }

            if (hitPoint != null) {
                break;
            }

            velocity = velocity.scale(drag).subtract(0, gravity, 0);
            currentPos = nextPos;
        }

        // Отрисовка Billboard AR-маркера
        if (hitPoint != null) {
            double distance = cameraPos.distanceTo(hitPoint);
            float scale = (float) Math.max(0.06, Math.min(distance * 0.016, 0.42));

            poseStack.pushPose();
            poseStack.translate(hitPoint.x - cameraPos.x, hitPoint.y - cameraPos.y, hitPoint.z - cameraPos.z);
            poseStack.mulPose(camera.rotation());

            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
            VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());

            drawTacticalHUD(poseStack.last(), buffer, scale, hitEntity);

            bufferSource.endBatch(RenderType.lines());
            poseStack.popPose();
        }
    }

    /**
     * Отрисовка тактического прицела
     */
    private static void drawTacticalHUD(PoseStack.Pose pose, VertexConsumer buffer, float s, boolean isEntity) {
        float r = isEntity ? 1.0F : 0.0F;
        float g = isEntity ? 0.2F : 0.95F;
        float b = isEntity ? 0.3F : 1.0F;
        float alpha = isEntity ? 1.0F : 0.85F;

        float corner = s * 0.35F;
        float dot = s * 0.12F;
        float tick = s * 0.15F;

        // 1. Центральное перекрестие (+)
        draw2DLine(pose, buffer, -dot, 0, dot, 0, r, g, b, alpha);
        draw2DLine(pose, buffer, 0, -dot, 0, dot, r, g, b, alpha);

        // 2. Угловые рамки [ ]
        draw2DLine(pose, buffer, -s, s, -s + corner, s, r, g, b, alpha);
        draw2DLine(pose, buffer, -s, s, -s, s - corner, r, g, b, alpha);

        draw2DLine(pose, buffer, s, s, s - corner, s, r, g, b, alpha);
        draw2DLine(pose, buffer, s, s, s, s - corner, r, g, b, alpha);

        draw2DLine(pose, buffer, -s, -s, -s + corner, -s, r, g, b, alpha);
        draw2DLine(pose, buffer, -s, -s, -s, -s + corner, r, g, b, alpha);

        draw2DLine(pose, buffer, s, -s, s - corner, -s, r, g, b, alpha);
        draw2DLine(pose, buffer, s, -s, s, -s + corner, r, g, b, alpha);

        // 3. Засечки разброса
        draw2DLine(pose, buffer, 0, s + 0.02F, 0, s + 0.02F + tick, r, g, b, alpha * 0.6F);
        draw2DLine(pose, buffer, 0, -s - 0.02F, 0, -s - 0.02F - tick, r, g, b, alpha * 0.6F);
        draw2DLine(pose, buffer, -s - 0.02F, 0, -s - 0.02F - tick, 0, r, g, b, alpha * 0.6F);
        draw2DLine(pose, buffer, s + 0.02F, 0, s + 0.02F + tick, 0, r, g, b, alpha * 0.6F);

        // 4. Ромб захвата цели
        if (isEntity) {
            float d = s * 1.35F;
            draw2DLine(pose, buffer, 0, d, d, 0, r, g, b, 0.8F);
            draw2DLine(pose, buffer, d, 0, 0, -d, r, g, b, 0.8F);
            draw2DLine(pose, buffer, 0, -d, -d, 0, r, g, b, 0.8F);
            draw2DLine(pose, buffer, -d, 0, 0, d, r, g, b, 0.8F);
        }
    }

    private static void draw2DLine(PoseStack.Pose pose, VertexConsumer buffer, float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        Matrix4f mat = pose.pose();
        Matrix3f norm = pose.normal();

        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 0.0001F) {
            dx /= len;
            dy /= len;
        } else {
            dy = 1.0F;
        }

        buffer.vertex(mat, x1, y1, 0.0F)
                .color(r, g, b, a)
                .normal(norm, dx, dy, 0.0F)
                .endVertex();

        buffer.vertex(mat, x2, y2, 0.0F)
                .color(r, g, b, a)
                .normal(norm, dx, dy, 0.0F)
                .endVertex();
    }

    /**
     * ПРОВЕРКА: Нужны базовые Киберглаза (cybereyes) + Ручной привод (arm_upgrades_bow).
     * Креатив-байпас полностью исключен.
     */
    public static boolean isFullTargetingSystemActive(Player player) {
        if (player == null) return false;

        // 1. Ручной модуль (arm_upgrades_bow)
        boolean hasArmBow = hasCyberware(player, "cyber_ware_port:arm_upgrades_bow");

        // 2. Базовые кибер-глаза (cybereyes)
        boolean hasBaseEyes = hasCyberware(player, "cyber_ware_port:cybereyes");

        return hasArmBow && hasBaseEyes;
    }

    /**
     * Безопасное чтение имплантов игрока
     */
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
                    Class<?> clazz = userData.getClass();
                    for (Field field : clazz.getDeclaredFields()) {
                        field.setAccessible(true);
                        Object val = field.get(userData);
                        if (checkContainerForId(val, targetId)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean checkContainerForId(Object obj, String targetId) {
        if (obj == null) return false;

        if (obj instanceof ItemStack stack && !stack.isEmpty()) {
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(targetId);
        }

        if (obj instanceof IItemHandler handler) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    if (id.equals(targetId)) return true;
                }
            }
        } else if (obj instanceof Iterable<?> iterable) {
            for (Object itemObj : iterable) {
                if (checkContainerForId(itemObj, targetId)) return true;
            }
        } else if (obj instanceof Object[] array) {
            for (Object itemObj : array) {
                if (checkContainerForId(itemObj, targetId)) return true;
            }
        } else if (obj instanceof Map<?, ?> map) {
            for (Object itemObj : map.values()) {
                if (checkContainerForId(itemObj, targetId)) return true;
            }
        }
        return false;
    }
}