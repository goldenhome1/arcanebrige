package com.example.arcanebridge.client;

import com.example.arcanebridge.logic.CyberwareHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@Mod.EventBusSubscriber(modid = "arcane_bridge", value = Dist.CLIENT)
public class MapImplantHandler {

    private static final List<String> BLOCKED_MAP_KEYWORDS = List.of(
            "xaero",
            "journeymap",
            "ftbchunks",
            "mapatlases"
    );

    private static long lastWarningTime = 0;
    private static boolean loggedReflectionStatus = false;

    /**
     * 1. Блокировка 2D-оверлеев миникарты (HUD)
     */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        String overlayId = event.getOverlay().id().toString().toLowerCase();

        // Пропускаем оверлеи меток, блокируем только саму карту
        if (overlayId.contains("waypoint")) return;

        for (String keyword : BLOCKED_MAP_KEYWORDS) {
            if (overlayId.contains(keyword)) {
                Player player = Minecraft.getInstance().player;
                if (player != null && !hasMapImplant(player)) {
                    event.setCanceled(true);
                }
                break;
            }
        }
    }

    /**
     * 2. Перехват открытия GUI (Карта мира блокируется, меню меток разрешено)
     */
    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        Screen screen = event.getNewScreen();
        if (screen == null) return;

        if (isBlockedScreen(screen)) {
            Player player = Minecraft.getInstance().player;
            if (player != null && !hasMapImplant(player)) {
                event.setCanceled(true);
                showWarning(player);
            }
        }
    }

    /**
     * 3. Тикер клиента: управление состоянием миникарты и принудительное закрытие карт
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        boolean hasImplant = hasMapImplant(player);

        // Отключаем только саму миникарту в памяти Xaero (метки не трогаем)
        forceDisableXaeroMinimap(!hasImplant);

        // Принудительный сброс экрана карты без импланта
        if (!hasImplant && mc.screen != null && isBlockedScreen(mc.screen)) {
            mc.setScreen(null);
            showWarning(player);
        }
    }

    /**
     * Проверка блокировки экрана: окна вэйпоинтов полностью исключены из бана
     */
    private static boolean isBlockedScreen(Screen screen) {
        if (screen == null) return false;
        String className = screen.getClass().getName().toLowerCase();

        // Разрешаем окна создания, редактирования, телепортации и шаринга меток
        if (className.contains("waypoint")) {
            return false;
        }

        for (String keyword : BLOCKED_MAP_KEYWORDS) {
            if (className.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Управление отображением оверлея миникарты Xaero через Reflection
     */
    private static void forceDisableXaeroMinimap(boolean disable) {
        try {
            Class<?> xaeroClass = Class.forName("xaero.common.XaeroMinimap");
            Object instance = xaeroClass.getField("instance").get(null);
            if (instance == null) return;

            Method getSettings = xaeroClass.getMethod("getSettings");
            Object settings = getSettings.invoke(instance);
            if (settings == null) return;

            Field minimapField = findField(settings.getClass(), "minimap");
            if (minimapField != null) {
                if (disable) {
                    minimapField.setBoolean(settings, false);
                } else if (HudConfig.showMap) {
                    minimapField.setBoolean(settings, true);
                }
            }
        } catch (Throwable t) {
            if (!loggedReflectionStatus) {
                System.out.println("[ArcaneBridge MapLock] Xaero Reflection note: " + t.getMessage());
                loggedReflectionStatus = true;
            }
        }
    }

    private static Field findField(Class<?> clazz, String... names) {
        for (String name : names) {
            try {
                Field f = clazz.getField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                try {
                    Field f = clazz.getDeclaredField(name);
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException ignoredInner) {}
            }
        }
        return null;
    }

    private static void showWarning(Player player) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastWarningTime > 1500) {
            lastWarningTime = currentTime;
            player.displayClientMessage(
                    Component.literal("§c[Нейроинтерфейс] Ошибка: Оптический модуль картографии не обнаружен!"),
                    true
            );
            player.level().playSound(
                    player,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_BREAK,
                    SoundSource.PLAYERS,
                    0.6F, 1.8F
            );
        }
    }

    public static boolean hasMapImplant(Player player) {
        if (player == null || !HudConfig.showMap) return false;
        return CyberwareHelper.isCyberwareHudActive(player);
    }
}