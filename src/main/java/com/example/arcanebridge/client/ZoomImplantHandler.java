package com.example.arcanebridge.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;

import java.lang.reflect.Field;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "arcane_bridge", value = Dist.CLIENT)
public class ZoomImplantHandler {

    private static final String ZOOM_IMPLANT_ID = "cyber_ware_port:cybereye_upgrades_zoom";
    private static double cleanFov = 70.0;
    private static long lastWarningTime = 0;

    /**
     * 1. В НАЧАЛЕ КАДРА (HIGHEST): Запоминаем чистый ванильный FOV (спринт, эффекты скорости, лук)
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onComputeFovPre(ViewportEvent.ComputeFov event) {
        cleanFov = event.getFOV();
    }

    /**
     * 2. В КОНЦЕ КАДРА (LOWEST): Глушим Shift-зум от Cyberware и блокируем сторонний зум без чипа
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onComputeFovPost(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        boolean hasZoom = hasZoomUpgradeInstalled(player);
        boolean isZoomPressed = isAnyZoomKeyPressed(mc);

        // Если импланта НЕТ — наглухо блокируем любые попытки изменить FOV
        if (!hasZoom) {
            event.setFOV(cleanFov);
            return;
        }

        // Если имплант ЕСТЬ — вырезаем только встроенный Shift-зум Cyberware
        if (player.isCrouching() && !isZoomPressed) {
            event.setFOV(cleanFov);
        }
    }

    /**
     * 3. Перехват кликов по клавишам сторонних модов зума
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.screen != null) return;

        for (KeyMapping keyMapping : mc.options.keyMappings) {
            String keyName = keyMapping.getName().toLowerCase();

            if (isZoomKey(keyName)) {
                if (keyMapping.isDown() || keyMapping.consumeClick()) {
                    if (!hasZoomUpgradeInstalled(player)) {
                        keyMapping.setDown(false);
                        showWarning(player);
                    }
                }
            }
        }
    }

    /**
     * 4. Тикер: удержание клавиши зума в сброшенном состоянии, если нет импланта
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        if (!hasZoomUpgradeInstalled(player)) {
            for (KeyMapping keyMapping : mc.options.keyMappings) {
                if (isZoomKey(keyMapping.getName().toLowerCase()) && keyMapping.isDown()) {
                    keyMapping.setDown(false);
                }
            }
        }
    }

    private static boolean isZoomKey(String keyName) {
        return keyName.contains("zoom") && !keyName.contains("arcane_bridge");
    }

    private static boolean isAnyZoomKeyPressed(Minecraft mc) {
        for (KeyMapping keyMapping : mc.options.keyMappings) {
            if (isZoomKey(keyMapping.getName().toLowerCase()) && keyMapping.isDown()) {
                return true;
            }
        }
        return false;
    }

    private static void showWarning(Player player) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastWarningTime > 1500) {
            lastWarningTime = currentTime;

            player.displayClientMessage(
                    Component.literal("§c[Сбой оптики]: Требуется имплант [Дистанционный усилитель]!"),
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

    /**
     * 5. Безопасный сканер слотов Cyberware
     */
    public static boolean hasZoomUpgradeInstalled(Player player) {
        if (player == null) return false;

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
                        if (checkContainerForId(val, ZOOM_IMPLANT_ID)) {
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