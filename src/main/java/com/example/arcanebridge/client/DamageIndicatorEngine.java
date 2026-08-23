package com.example.arcanebridge.client;

import com.example.arcanebridge.logic.CyberwareHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "arcane_bridge", value = Dist.CLIENT)
public class DamageIndicatorEngine {

    private static final int MAX_POPUPS = 24; // Лимит одновременных меток на экране
    private static final List<DamagePopup> POPUPS = new ArrayList<>(MAX_POPUPS);
    private static final Object LOCK = new Object();

    private static class DamagePopup {
        final int entityId;
        double x, y, z;
        float mx, my, mz;
        float totalDamage;
        boolean isCrit;
        int age;
        final int maxAge;

        // Кэшированные графические данные (вычисляются 1 раз при создании)
        String cachedText;
        float cachedTextWidth;
        int cachedBaseColor;
        float scale;

        DamagePopup(int entityId, double spawnX, double spawnY, double spawnZ, float damage, boolean isCrit, Font font) {
            this.entityId = entityId;

            // Радиальный разлет на примитивах
            double angle = Math.random() * 6.283185307179586; // 2 * PI
            float horizSpeed = (float) (0.035 + (Math.random() * 0.035));
            this.mx = (float) (Math.cos(angle) * horizSpeed);
            this.mz = (float) (Math.sin(angle) * horizSpeed);
            this.my = (float) (0.09 + (Math.random() * 0.03));

            this.x = spawnX + (Math.random() - 0.5) * 0.2;
            this.y = spawnY;
            this.z = spawnZ + (Math.random() - 0.5) * 0.2;

            this.totalDamage = damage;
            this.isCrit = isCrit;
            this.age = 0;
            this.maxAge = 30;

            rebuildVisualCache(font);
        }

        void addDamage(float additionalDmg, boolean crit, Font font) {
            this.totalDamage += additionalDmg;
            if (crit) this.isCrit = true;
            this.mx *= 1.15f;
            this.my = (float) (0.05 + Math.random() * 0.02);
            this.mz *= 1.15f;
            this.age = Math.max(0, this.age - 8);

            rebuildVisualCache(font);
        }

        private void rebuildVisualCache(Font font) {
            int intPart = (int) totalDamage;
            float fracPart = totalDamage - intPart;

            // Быстрая склейка без тяжелого String.format
            if (fracPart < 0.05f) {
                this.cachedText = isCrit ? ("★ " + intPart) : Integer.toString(intPart);
            } else {
                int firstDecimal = (int) (fracPart * 10.0f);
                this.cachedText = isCrit ? ("★ " + intPart + "." + firstDecimal) : (intPart + "." + firstDecimal);
            }

            this.cachedTextWidth = font.width(this.cachedText);

            if (isCrit) {
                this.cachedBaseColor = 0x00FF3333; // Красный
            } else if (totalDamage >= 15.0f) {
                this.cachedBaseColor = 0x00D841DB; // Фиолетовый
            } else if (totalDamage >= 6.0f) {
                this.cachedBaseColor = 0x00FFCC00; // Золотой
            } else {
                this.cachedBaseColor = 0x0000E5FF; // Лазурный
            }

            this.scale = totalDamage >= 15.0f ? 0.034F : 0.025F;
        }

        boolean tick() {
            age++;
            x += mx;
            y += my;
            z += mz;
            mx *= 0.90f;
            my -= 0.0045f; // гравитация
            mz *= 0.90f;
            return age >= maxAge;
        }
    }

    @SubscribeEvent
    public static void onEntityDamage(LivingDamageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        if (!CyberwareHelper.isCyberwareHudActive(player) || !HudConfig.showDamageNumbers) {
            return;
        }

        LivingEntity target = event.getEntity();
        if (target == null || target == player) return;

        float amount = event.getAmount();
        if (amount <= 0.1f) return;

        // Отсечение по дистанции: до 24 блоков (576 в квадрате)
        if (player.distanceToSqr(target) > 576.0) return;

        boolean isCrit = player.fallDistance > 0.0F && !player.onGround() && !player.onClimbable();
        Font font = mc.font;

        synchronized (LOCK) {
            // Мгновенное слияние дробинок дробовика
            for (int i = 0; i < POPUPS.size(); i++) {
                DamagePopup popup = POPUPS.get(i);
                if (popup.entityId == target.getId() && popup.age < 12) {
                    popup.addDamage(amount, isCrit, font);
                    return;
                }
            }

            // Контроль пула: если переполнен, удаляем самый старый
            if (POPUPS.size() >= MAX_POPUPS) {
                POPUPS.remove(0);
            }

            double spawnY = target.getY() + target.getBbHeight() + 0.35;
            POPUPS.add(new DamagePopup(
                    target.getId(),
                    target.getX(),
                    spawnY,
                    target.getZ(),
                    amount,
                    isCrit,
                    font
            ));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (!HudConfig.showDamageNumbers) {
            if (!POPUPS.isEmpty()) {
                synchronized (LOCK) {
                    POPUPS.clear();
                }
            }
            return;
        }

        if (POPUPS.isEmpty()) return;

        synchronized (LOCK) {
            POPUPS.removeIf(DamagePopup::tick);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (POPUPS.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !CyberwareHelper.isCyberwareHudActive(player) || !HudConfig.showDamageNumbers) return;

        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        Font font = mc.font;
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        synchronized (LOCK) {
            for (int i = 0; i < POPUPS.size(); i++) {
                DamagePopup popup = POPUPS.get(i);

                double rx = popup.x - cameraPos.x;
                double ry = popup.y - cameraPos.y;
                double rz = popup.z - cameraPos.z;

                // Не рендерим то, что за пределами разумного радиуса
                if (rx * rx + ry * ry + rz * rz > 576.0) continue;

                poseStack.pushPose();
                poseStack.translate(rx, ry, rz);
                poseStack.mulPose(event.getCamera().rotation());

                float s = popup.scale;
                poseStack.scale(-s, -s, s);

                Matrix4f matrix4f = poseStack.last().pose();

                // Быстрый альфа-канал
                float alpha = 1.0F - ((float) popup.age / (float) popup.maxAge);
                int alphaBits = ((int) (alpha * 255.0F)) << 24;
                int finalColor = popup.cachedBaseColor | alphaBits;

                font.drawInBatch(
                        popup.cachedText,
                        -popup.cachedTextWidth * 0.5F,
                        0,
                        finalColor,
                        true,
                        matrix4f,
                        bufferSource,
                        Font.DisplayMode.NORMAL,
                        0,
                        15728880 // Полная яркость
                );

                poseStack.popPose();
            }
        }
        bufferSource.endBatch();
    }
}