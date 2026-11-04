package com.example.arcanebridge.client;

import com.example.arcanebridge.network.ClientboundResonanceSyncPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "arcane_bridge")
public class ResonanceVfxHandler {

    private static final ResourceLocation SHIELD_RUNES_TEX =
            new ResourceLocation("arcane_bridge", "textures/vfx/shield_runes.png");

    private static final Map<UUID, LinkedList<PosSnapshot>> POSITION_HISTORIES = new HashMap<>();
    private static int clientTickCounter = 0;
    private static float lastStability = 100.0f;
    private static int clientCrouchChargeTicks = 0;

    // Снимок положения для динамического растягивания и притягивания шлейфа
    public record PosSnapshot(double x, double y, double z, float bodyYaw) {}

    // Ровно 3 четко видимые копии с задержками 5, 10, 15 тиков
    private static final int[] SANDEVISTAN_DELAYS = new int[]{5, 10, 15};
    private static final int MAX_HISTORY_TICKS = 30;

    // Рефлексия для динамического создания светящихся частиц Ars Nouveau
    private static boolean arsReflectionChecked = false;
    private static Constructor<?> arsColorConstructor = null;
    private static Method arsCreateGlowMethod = null;

    private static void initArsReflection() {
        if (arsReflectionChecked) return;
        arsReflectionChecked = true;
        try {
            Class<?> colorClass = Class.forName("com.hollingsworth.arsnouveau.client.particle.ParticleColor");
            arsColorConstructor = colorClass.getConstructor(int.class, int.class, int.class);

            Class<?> glowDataClass = Class.forName("com.hollingsworth.arsnouveau.client.particle.GlowParticleData");
            arsCreateGlowMethod = glowDataClass.getMethod("createData", colorClass, float.class, float.class, int.class);
        } catch (Throwable ignored) {}
    }

    public static ParticleOptions getArsGlow(int r, int g, int b, float size, float alpha, int age) {
        initArsReflection();
        if (arsColorConstructor != null && arsCreateGlowMethod != null) {
            try {
                Object colorObj = arsColorConstructor.newInstance(r, g, b);
                return (ParticleOptions) arsCreateGlowMethod.invoke(null, colorObj, size, alpha, age);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    @SubscribeEvent
    public static void onClientLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        POSITION_HISTORIES.clear();
        clientCrouchChargeTicks = 0;
        ClientboundResonanceSyncPacket.tendrilStacksStatic = 0;
        ClientboundResonanceSyncPacket.tendrilAwakeningTicksStatic = 0;
        ClientboundResonanceSyncPacket.mechChargeBarsStatic = 0;
        ClientboundResonanceSyncPacket.tendrilCdStatic = 0;
        ClientboundResonanceSyncPacket.mechCdStatic = 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            POSITION_HISTORIES.clear();
            return;
        }

        clientTickCounter++;
        float stability = ClientboundResonanceSyncPacket.clientStability;
        boolean arcaneOverload = ClientboundResonanceSyncPacket.arcaneLoadStatic > ClientboundResonanceSyncPacket.arcaneLimitStatic;
        boolean eleOverload = ClientboundResonanceSyncPacket.eleLoadStatic > ClientboundResonanceSyncPacket.eleLimitStatic;

        // Шлейф и магия работают ТОЛЬКО для Арканы и Стихий (НИКОГДА для механики)!
        boolean isMagicOverloaded = (arcaneOverload || eleOverload) || (stability <= 70.0f && (ClientboundResonanceSyncPacket.arcaneLoadStatic > 0 || ClientboundResonanceSyncPacket.eleLoadStatic > 0));

        // 1. Атмосферный звуковой импульс при пересечении порога в Тир 3
        if (lastStability > 50.0f && stability <= 50.0f && isMagicOverloaded) {
            mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                    SoundEvents.RESPAWN_ANCHOR_DEPLETE.get(), SoundSource.PLAYERS, 0.7f, 1.7f, false);
        }
        lastStability = stability;

        int syncedStacks = ClientboundResonanceSyncPacket.tendrilStacksStatic;
        int awakeTicks = ClientboundResonanceSyncPacket.tendrilAwakeningTicksStatic;
        int tCd = ClientboundResonanceSyncPacket.tendrilCdStatic;

        // 2. Учет накопления заряда щупалец при удержании Shift (ТОЛЬКО если нет кулдауна!)
        if (player.isCrouching() && isMagicOverloaded && stability <= 50.0f && tCd <= 0) {
            if (clientCrouchChargeTicks < 100) {
                clientCrouchChargeTicks++;
            }
        } else if (!player.isCrouching() || stability > 50.0f || !isMagicOverloaded || tCd > 0 || (syncedStacks == 0 && awakeTicks == 0)) {
            clientCrouchChargeTicks = 0;
        }

        // 3. Непрерывная запись истории позиций для шлейфа: ТОЛЬКО критический коллапс Тир 5 (<= 20%)!
        if (stability <= 20.0f) {
            UUID id = player.getUUID();
            LinkedList<PosSnapshot> history = POSITION_HISTORIES.computeIfAbsent(id, k -> new LinkedList<>());
            history.addFirst(new PosSnapshot(player.getX(), player.getY(), player.getZ(), player.yBodyRot));

            while (history.size() > MAX_HISTORY_TICKS) {
                history.removeLast();
            }
        } else {
            POSITION_HISTORIES.remove(player.getUUID());
        }

        // 4. Дикие извивающиеся щупальца со спины: ТОЛЬКО магия и ТОЛЬКО Тир 3+ (<= 50.0%)!
        // В Тир 2 (<= 70% но > 50%) щупальца СТРОГО ОТКЛЮЧЕНЫ - работает только чистый шлейф!
        if (isMagicOverloaded && stability <= 50.0f) {
            spawnUnstableTendrils(mc, player, stability, arcaneOverload, eleOverload);
        }
    }

    /**
     * Дикие эфирные щупальца маны, вырывающиеся СТРОГО ИЗ СПИНЫ / ЛОПАТОК игрока.
     * При зажатом Shift визуально стакаются от 1 до 4 щупалец, готовых к удару!
     */
    private static void spawnUnstableTendrils(Minecraft mc, Player player, float stability,
                                              boolean arcane, boolean ele) {
        if (player.level() == null) return;

        // Подбор цветов Ars Nouveau
        ParticleOptions bodyParticle;
        ParticleOptions tipParticle;
        if (ele) {
            bodyParticle = getArsGlow(40, 220, 255, 0.32F, 0.85F, 16);
            tipParticle = getArsGlow(255, 140, 40, 0.45F, 1.0F, 18);
        } else {
            bodyParticle = getArsGlow(215, 75, 255, 0.32F, 0.85F, 16);
            tipParticle = getArsGlow(245, 120, 255, 0.45F, 1.0F, 18);
        }

        if (bodyParticle == null) bodyParticle = ele ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.REVERSE_PORTAL;
        if (tipParticle == null) tipParticle = ele ? ParticleTypes.FLAME : ParticleTypes.PORTAL;

        // Векторы ориентации тела игрока
        float bodyYaw = player.yBodyRot;
        double yawRad = Math.toRadians(bodyYaw);
        double backX = Math.sin(yawRad);
        double backZ = -Math.cos(yawRad);
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);

        int syncedStacks = ClientboundResonanceSyncPacket.tendrilStacksStatic;
        int awakeTicks = ClientboundResonanceSyncPacket.tendrilAwakeningTicksStatic;
        int tCd = ClientboundResonanceSyncPacket.tendrilCdStatic;
        boolean hasSatiation = player.hasEffect(com.example.arcanebridge.registry.ModEffects.CRYSTALLINE_SATIATION.get());
        int tendrilCount;
        float envelope;
        boolean isCharging = (awakeTicks > 0) || (syncedStacks > 0);

        if (awakeTicks > 0) {
            tendrilCount = Math.max(1, syncedStacks > 0 ? syncedStacks : 3);
            envelope = 1.0F;
        } else if (syncedStacks > 0) {
            tendrilCount = syncedStacks;
            envelope = 1.0F;
        } else {
            // Если способность на перезарядке или насыщена — щупальца отдыхают и не отображаются!
            if (hasSatiation || tCd > 0) return;

            // Периодичность фоновых выбросов: Тир 3 = каждые ~9 сек (180 тиков), Тир 4-5 = каждые ~6 сек (120 тиков)
            int cycleLength = stability <= 35.0F ? 120 : 180;
            int burstDuration = 32;
            int cycle = clientTickCounter % cycleLength;

            if (cycle < 8) {
                envelope = cycle / 8.0F; // Взрывной выход из лопаток
            } else if (cycle < 24) {
                envelope = 1.0F; // Бурный взмах в воздухе
            } else if (cycle < burstDuration) {
                envelope = 1.0F - (cycle - 24) / 8.0F; // Растворение обратно в тело
            } else {
                // Спокойная фаза: искры на лопатках за спиной
                if (cycle % 12 == 0) {
                    double side = (Math.random() < 0.5 ? -1.0 : 1.0) * 0.20;
                    double sx = player.getX() + backX * 0.18 + rightX * side;
                    double sy = player.getY() + 1.15 + (Math.random() - 0.5) * 0.1;
                    double sz = player.getZ() + backZ * 0.18 + rightZ * side;
                    mc.level.addParticle(bodyParticle, sx, sy, sz, 0, 0.015, 0);
                }
                return;
            }

            int burstId = (clientTickCounter / cycleLength) % 3;
            if (stability <= 35.0F) {
                tendrilCount = (burstId == 2) ? 3 : 2;
            } else {
                tendrilCount = (burstId == 2) ? 2 : 1;
            }
        }

        double baseLength = (stability <= 35.0F ? 2.3D : 1.85D) * envelope;
        if (isCharging) {
            baseLength *= (1.0D + tendrilCount * 0.08D); // Заряженные щупальца массивнее
        }

        for (int t = 0; t < tendrilCount; t++) {
            double side;
            double sideSign;
            double lenMult;
            double freqMult;

            if (tendrilCount == 1) {
                sideSign = -1.0;
                side = -0.20;
                lenMult = 1.0;
                freqMult = 1.0;
            } else if (tendrilCount == 2) {
                if (t == 0) {
                    sideSign = -1.0; side = -0.20; lenMult = 1.08; freqMult = 1.0;
                } else {
                    sideSign = 1.0; side = 0.20; lenMult = 0.85; freqMult = 1.35;
                }
            } else if (tendrilCount == 3) {
                if (t == 0) {
                    sideSign = -1.0; side = -0.22; lenMult = 1.15; freqMult = 1.0;
                } else if (t == 1) {
                    sideSign = 1.0; side = 0.22; lenMult = 0.90; freqMult = 1.4;
                } else {
                    sideSign = 0.0; side = 0.0; lenMult = 1.25; freqMult = 0.75;
                }
            } else {
                // 4 щупальца при максимальном заряде Shift!
                if (t == 0) {
                    sideSign = -1.0; side = -0.24; lenMult = 1.22; freqMult = 1.0;
                } else if (t == 1) {
                    sideSign = 1.0; side = 0.24; lenMult = 1.22; freqMult = 1.18;
                } else if (t == 2) {
                    sideSign = -1.0; side = -0.10; lenMult = 0.95; freqMult = 1.40;
                } else {
                    sideSign = 1.0; side = 0.10; lenMult = 0.95; freqMult = 1.55;
                }
            }

            double tendrilLen = baseLength * lenMult;
            double time = clientTickCounter * (0.22D * freqMult);

            int segments = 7;
            for (int s = 1; s <= segments; s++) {
                double progress = (double) s / segments;
                double curDist = tendrilLen * progress;

                // 3D-волна извивания
                double waveSide = Math.sin(time + progress * 3.8D + t * 2.1D) * 0.35D * progress;
                double waveY = Math.cos(time * 0.85D + progress * 2.6D + t * 1.6D) * 0.32D * progress;

                // СТРОГО ЗА СПИНОЙ: segBack всегда строго положителен (> 0.18), частицы никогда не летят вперед!
                double segBack = 0.18D + curDist * 0.82D;
                double segSide = side + (sideSign * curDist * 0.38D) + waveSide;

                double tx = player.getX() + backX * segBack + rightX * segSide;
                double ty = player.getY() + 1.15D + (progress * 0.85D) + waveY;
                double tz = player.getZ() + backZ * segBack + rightZ * segSide;

                double vx = backX * 0.02D + (Math.random() - 0.5) * 0.01D;
                double vy = 0.02D + waveY * 0.02D;
                double vz = backZ * 0.02D + (Math.random() - 0.5) * 0.01D;

                if (s == segments) {
                    mc.level.addParticle(tipParticle, tx, ty, tz, vx * 1.5D, vy, vz * 1.5D);
                } else {
                    mc.level.addParticle(bodyParticle, tx, ty, tz, vx * 0.5D, vy * 0.5D, vz * 0.5D);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof AbstractClientPlayer clientPlayer)) return;

        float stability = ClientboundResonanceSyncPacket.clientStability;
        boolean arcaneOverload = ClientboundResonanceSyncPacket.arcaneLoadStatic > ClientboundResonanceSyncPacket.arcaneLimitStatic;
        boolean eleOverload = ClientboundResonanceSyncPacket.eleLoadStatic > ClientboundResonanceSyncPacket.eleLimitStatic;

        // Шлейф Сандевистан активен ТОЛЬКО при критической нестабильности <= 20% (Тир 5)!
        // При коллапсе реальности тело игрока рассинхронизируется в пространстве
        if (stability > 20.0f) {
            return;
        }

        LinkedList<PosSnapshot> history = POSITION_HISTORIES.get(player.getUUID());
        if (history == null || history.size() < 4) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        ResourceLocation skinTexture = event.getRenderer().getTextureLocation(clientPlayer);

        long now = System.currentTimeMillis();

        // Интерполированная позиция игрока для субтиковой плавности
        float pt = event.getPartialTick();
        double curX = Mth.lerp(pt, player.xo, player.getX());
        double curY = Mth.lerp(pt, player.yo, player.getY());
        double curZ = Mth.lerp(pt, player.zo, player.getZ());

        // Единый чистый цвет без градиента
        float r = 0.85F, g = 0.35F, b = 1.0F; // Арканный неон
        if (eleOverload) {
            r = 0.20F; g = 0.90F; b = 1.0F; // Стихийный лазурный
        }

        // Рендерим ровно 3 четко видимые копии (задержки 5, 10, 15 тиков)
        for (int i = 0; i < SANDEVISTAN_DELAYS.length; i++) {
            int delay = SANDEVISTAN_DELAYS[i];
            if (delay >= history.size()) continue;

            PosSnapshot curr = history.get(delay);
            PosSnapshot prev = (delay + 1 < history.size()) ? history.get(delay + 1) : curr;

            double echoX = Mth.lerp(pt, curr.x(), prev.x());
            double echoY = Mth.lerp(pt, curr.y(), prev.y());
            double echoZ = Mth.lerp(pt, curr.z(), prev.z());
            float echoYaw = Mth.rotLerp(pt, curr.bodyYaw(), prev.bodyYaw());

            double dx = echoX - curX;
            double dy = echoY - curY;
            double dz = echoZ - curZ;

            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < 0.003D || distSq > 80.0D) continue;

            double dist = Math.sqrt(distSq);
            // Плавное проявление при выходе из тела
            float emergence = (float) Mth.clamp(dist / 0.16D, 0.0D, 1.0D);

            // Оптимальная прозрачность: все 3 копии четко различимы в игре!
            float baseAlpha = switch (i) {
                case 0 -> 0.38F;
                case 1 -> 0.28F;
                default -> 0.20F;
            };
            float fade = baseAlpha * emergence;

            poseStack.pushPose();
            // 1. Перенос в мировую позицию следа
            poseStack.translate(dx, dy, dz);

            // 2. Поворот по направлению движения тела в момент создания копии
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - echoYaw));

            // 3. Исправление инверсии модели (прямо на ногах)
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.scale(0.9375F, 0.9375F, 0.9375F);
            poseStack.translate(0.0D, -1.501D, 0.0D);

            // 4. Полупрозрачный силуэт модели игрока
            VertexConsumer skinBuffer = bufferSource.getBuffer(RenderType.entityTranslucent(skinTexture));
            model.renderToBuffer(poseStack, skinBuffer, event.getPackedLight(), OverlayTexture.NO_OVERLAY, r, g, b, fade);

            // 5. Аддитивный слой рунической матрицы
            float timeSec = (now % 2000) / 2000.0F;
            VertexConsumer runeBuffer = bufferSource.getBuffer(RenderType.energySwirl(SHIELD_RUNES_TEX, timeSec + i * 0.25F, timeSec));
            model.renderToBuffer(poseStack, runeBuffer, 15728880, OverlayTexture.NO_OVERLAY, r, g, b, fade * 0.70F);

            poseStack.popPose();
        }
    }

    private static final ResourceLocation RESONANCE_HUD_TEX =
            new ResourceLocation("arcane_bridge", "textures/gui/resonance_hud.png");

    /**
     * Компактный индикатор слева от хотбара:
     * - Располагается слева от 1-го слота хотбара на чистом свободном пространстве экрана.
     * - НЕ перекрывает сердечки, полоску брони, еду и шкалу опыта.
     * - Активен ТОЛЬКО при зарядке на Shift, удержании заряда (10 сек) или в боевом режиме (5 сек).
     * - В обычном спокойном состоянии полностью скрыт и не мешает игре.
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!VanillaGuiOverlay.HOTBAR.type().equals(event.getOverlay())) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        int tStacks = ClientboundResonanceSyncPacket.tendrilStacksStatic;
        int tAwake = ClientboundResonanceSyncPacket.tendrilAwakeningTicksStatic;
        int tCd = ClientboundResonanceSyncPacket.tendrilCdStatic;
        int tMaxCd = ClientboundResonanceSyncPacket.tendrilMaxCdStatic;

        int mBars = ClientboundResonanceSyncPacket.mechChargeBarsStatic;
        boolean hasOverclock = player.hasEffect(com.example.arcanebridge.registry.ModEffects.KINETIC_OVERCLOCK.get());
        int mCd = ClientboundResonanceSyncPacket.mechCdStatic;
        int mMaxCd = ClientboundResonanceSyncPacket.mechMaxCdStatic;

        // Не мешаем обычной игре: скрыт при нулевом заряде, нулевом боевом таймере и отсутствии перезарядки
        if (tStacks == 0 && tAwake == 0 && tCd == 0 && mBars == 0 && !hasOverclock && mCd == 0) return;

        GuiGraphics g = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();

        int hotbarX = (width - 182) / 2;
        // Позиция виджета: слева от хотбара на свободном месте экрана
        int widgetX = hotbarX - 18;
        int widgetY = height - 24;

        boolean eleOverload = ClientboundResonanceSyncPacket.eleLoadStatic > ClientboundResonanceSyncPacket.eleLimitStatic;

        if (tStacks > 0 || tAwake > 0 || tCd > 0) {
            renderArcaneGauge(g, widgetX, widgetY, tStacks, tAwake, tCd, tMaxCd, eleOverload);
        } else if (mBars > 0 || hasOverclock || mCd > 0) {
            int duration = hasOverclock ? player.getEffect(com.example.arcanebridge.registry.ModEffects.KINETIC_OVERCLOCK.get()).getDuration() : 0;
            renderMechGauge(g, widgetX, widgetY, mBars, duration, mCd, mMaxCd);
        }
    }

    private static void renderMechGauge(GuiGraphics g, int x, int y, int bars, int durationTicks, int cdTicks, int maxCd) {
        long time = System.currentTimeMillis();

        // 1. Отрисовка латунного корпуса манометра (14 x 24)
        g.blit(RESONANCE_HUD_TEX, x, y, 0, 0, 14, 24);

        // 2. Внутренняя камера: x+3, y+4, ширина 8, высота 16 (4 деления по 4px)
        int fillH = 0;
        int fillU = 16; // Обычное медное давление пара

        if (durationTicks > 0) {
            fillU = 26; // Раскаленный форсаж (overclock)
            float progress = Math.min(1.0f, Math.max(0.0f, durationTicks / 100.0f));
            fillH = Math.round(16 * progress);

            // Пульсирующее предупреждение на последних 1.5 сек (<= 30 тиков)
            if (durationTicks <= 30) {
                float pulse = (float) Math.sin(time / 50.0) * 0.5f + 0.5f;
                if (pulse <= 0.25f) return;
            }
        } else if (bars > 0) {
            fillH = Math.min(16, bars * 4);
        } else if (cdTicks > 0) {
            // Перезарядка: остывание манометра и набор давления (заполнение до готовности)
            fillU = 88;
            float progress = 1.0f - Math.min(1.0f, Math.max(0.0f, (float) cdTicks / Math.max(1, maxCd)));
            fillH = Math.round(16 * progress);
        }

        if (fillH > 0) {
            int renderY = (y + 4) + (16 - fillH);
            int texV = 4 + (16 - fillH);
            g.blit(RESONANCE_HUD_TEX, x + 3, renderY, fillU, texV, 8, fillH);
        }
    }

    private static void renderArcaneGauge(GuiGraphics g, int x, int y, int stacks, int awakeTicks, int cdTicks, int maxCd, boolean ele) {
        long time = System.currentTimeMillis();

        // 1. Отрисовка обсидиановой оправы кристалла (14 x 24)
        g.blit(RESONANCE_HUD_TEX, x, y, 40, 0, 14, 24);

        // 2. Внутренняя область кристалла: x+3, y+4, ширина 8, высота 16 (4 грани по 4px)
        int fillH = 0;
        int fillU = ele ? 76 : 56; // Стихийный (76) или арканный (56)

        if (awakeTicks > 0) {
            fillU = ele ? 76 : 66; // Стихийный или пробужденный астральный (66)
            float progress = Math.min(1.0f, Math.max(0.0f, awakeTicks / 100.0f));
            fillH = Math.round(16 * progress);

            if (awakeTicks <= 30) {
                float pulse = (float) Math.sin(time / 60.0) * 0.5f + 0.5f;
                if (pulse <= 0.25f) return;
            }
        } else if (stacks > 0) {
            fillH = Math.min(16, stacks * 4);
        } else if (cdTicks > 0) {
            // Перезарядка: кристалл остывает / заполняется снизу вверх до полной готовности!
            fillU = 98;
            float progress = 1.0f - Math.min(1.0f, Math.max(0.0f, (float) cdTicks / Math.max(1, maxCd)));
            fillH = Math.round(16 * progress);
        }

        if (fillH > 0) {
            int renderY = (y + 4) + (16 - fillH);
            int texV = 4 + (16 - fillH);
            g.blit(RESONANCE_HUD_TEX, x + 3, renderY, fillU, texV, 8, fillH);
        }
    }

    public static void handleTendrilStrikePacket(double startX, double startY, double startZ,
                                                 double endX, double endY, double endZ,
                                                 int stacks, boolean isElemental) {
        net.minecraft.world.level.Level level = Minecraft.getInstance().level;
        if (level == null) return;

        net.minecraft.core.particles.ParticleOptions bodyParticle;
        net.minecraft.core.particles.ParticleOptions tipParticle;

        if (isElemental) {
            bodyParticle = getArsGlow(40, 220, 255, 0.38F, 0.90F, 20);
            tipParticle = getArsGlow(255, 140, 40, 0.52F, 1.0F, 24);
        } else {
            bodyParticle = getArsGlow(215, 75, 255, 0.38F, 0.90F, 20);
            tipParticle = getArsGlow(245, 120, 255, 0.52F, 1.0F, 24);
        }

        if (bodyParticle == null) bodyParticle = net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL;
        if (tipParticle == null) tipParticle = net.minecraft.core.particles.ParticleTypes.PORTAL;

        double dx = endX - startX;
        double dy = endY - startY;
        double dz = endZ - startZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = Math.max(10, (int) (dist * 9));

        // Спавним дуги светящихся щупалец Ars Nouveau от спины игрока к цели
        for (int t = 0; t < stacks; t++) {
            double sideAngle = (t - (stacks - 1) * 0.5D) * 0.75D;
            for (int s = 0; s <= steps; s++) {
                double prog = (double) s / steps;
                double arcY = Math.sin(prog * Math.PI) * (1.25D + t * 0.40D);
                double arcSide = Math.sin(prog * Math.PI) * sideAngle;

                double lx = startX + dx * prog + (-dz / Math.max(0.1, dist)) * arcSide;
                double ly = startY + dy * prog + arcY;
                double lz = startZ + dz * prog + (dx / Math.max(0.1, dist)) * arcSide;

                double vx = (Math.random() - 0.5) * 0.02D;
                double vy = (Math.random() - 0.5) * 0.02D;
                double vz = (Math.random() - 0.5) * 0.02D;

                level.addParticle(s == steps ? tipParticle : bodyParticle, lx, ly, lz, vx, vy, vz);
            }
        }

        // Вспышка и радиальный разлет светящихся частиц Ars Nouveau на цели
        int burstCount = 15 + stacks * 8;
        for (int i = 0; i < burstCount; i++) {
            double speed = 0.08D + Math.random() * 0.18D;
            double ang1 = Math.random() * Math.PI * 2.0D;
            double ang2 = (Math.random() - 0.5D) * Math.PI;

            double vx = Math.cos(ang2) * Math.cos(ang1) * speed;
            double vy = Math.sin(ang2) * speed + 0.04D;
            double vz = Math.cos(ang2) * Math.sin(ang1) * speed;

            level.addParticle(i % 2 == 0 ? tipParticle : bodyParticle, endX, endY, endZ, vx, vy, vz);
        }
    }
}
