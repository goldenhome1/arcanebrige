package com.example.arcanebridge.logic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.items.IItemHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

public class CyberwareHelper {

    private static final Set<String> HUD_IMPLANTS = Set.of(
            "cyber_ware_port:cybereyes",
            "cyber_ware_port:cybereye_upgrades_hudjack",
            "cyber_ware_port:hud_jack",
            "cyber_ware_port:hud_lens"
    );

    public static boolean isCyberwareHudActive(Player player) {
        if (player == null) return false;

        // Если мы на клиенте — можем спросить оверлей Cyberware
        if (FMLEnvironment.dist == Dist.CLIENT && player.level().isClientSide()) {
            try {
                Class<?> providerClass = Class.forName("com.maxwell.cyber_ware_port.common.capability.CyberwareCapabilityProvider");
                Field capField = providerClass.getField("CYBERWARE_CAPABILITY");
                net.minecraftforge.common.capabilities.Capability<?> cap =
                        (net.minecraftforge.common.capabilities.Capability<?>) capField.get(null);

                var lazyOpt = player.getCapability(cap);
                if (lazyOpt.isPresent()) {
                    Object userData = lazyOpt.orElse(null);
                    if (userData != null) {
                        Class<?> overlayClass = Class.forName("com.maxwell.cyber_ware_port.client.upgrades.cybereye.CyberwareHudOverlay");
                        Method isHudActiveMethod = overlayClass.getDeclaredMethod("isHudActive", userData.getClass());
                        isHudActiveMethod.setAccessible(true);
                        return (boolean) isHudActiveMethod.invoke(null, userData);
                    }
                }
            } catch (Throwable ignored) {}
        }

        // На сервере (или если оверлей не найден) проверяем физическое наличие импланта
        return hasAnyHudImplant(player);
    }

    private static boolean hasAnyHudImplant(Player player) {
        for (String id : HUD_IMPLANTS) {
            if (hasCyberware(player, id)) return true;
        }
        return false;
    }

    public static boolean hasCyberware(Player player, String targetId) {
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