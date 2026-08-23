package com.example.arcanebridge.logic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class MiningImplantHandler {

    private static final String MINING_IMPLANT_ID = "cyber_ware_port:hand_upgrades_mining";
    private static final int ENERGY_COST_PER_BLOCK = 10; // 10 FE за блок в жиле

    private static final Map<UUID, Long> LAST_ALERT_TIME = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (player.isSpectator()) return;

        // Проверяем вызов от алгоритма FTB Ultimine
        if (isFTBUltimineBreak()) {
            // 1. Проверяем наличие Усиленного кулака (hand_upgrades_mining)
            if (!hasCyberware(player, MINING_IMPLANT_ID)) {
                event.setCanceled(true);
                sendFailureFeedback(player, "[Сбой привода]: Требуется имплант [Усиленный кулак]", false);
                return;
            }

            // 2. Проверяем запас внутренней энергии Cyberware
            int currentPower = getCyberwarePower(player);
            if (currentPower < ENERGY_COST_PER_BLOCK) {
                event.setCanceled(true);
                sendFailureFeedback(player, "[Низкий заряд]: Недостаточно энергии для работы [Усиленного кулака] (Заряд: " + currentPower + " / " + ENERGY_COST_PER_BLOCK + " FE)", true);
                return;
            }

            // 3. Списываем 10 FE за блок
            extractCyberwarePower(player, ENERGY_COST_PER_BLOCK);

            // 4. Сбрасываем истощение FTB Ultimine, чтобы голод не списывался дважды
            resetUltimineHungerDrain(player);
        }
    }

    private static void resetUltimineHungerDrain(ServerPlayer player) {
        try {
            FoodData foodData = player.getFoodData();
            foodData.setExhaustion(0.0F);
        } catch (Throwable ignored) {}
    }

    private static boolean isFTBUltimineBreak() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            String className = element.getClassName().toLowerCase();
            if (className.contains("ftbultimine") || className.contains("ftb_ultimine")) {
                return true;
            }
        }
        return false;
    }

    private static void sendFailureFeedback(ServerPlayer player, String message, boolean isLowPower) {
        long now = System.currentTimeMillis();
        long lastTime = LAST_ALERT_TIME.getOrDefault(player.getUUID(), 0L);

        if (now - lastTime > 1200) {
            LAST_ALERT_TIME.put(player.getUUID(), now);

            player.displayClientMessage(Component.literal("§c" + message), true);

            ServerLevel level = player.serverLevel();
            if (isLowPower) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.PLAYERS, 0.8F, 1.2F);
            } else {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ANVIL_HIT, SoundSource.PLAYERS, 0.5F, 1.8F);
            }
        }
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

    private static int getCyberwarePower(Player player) {
        try {
            var energyCap = player.getCapability(ForgeCapabilities.ENERGY);
            if (energyCap.isPresent()) {
                IEnergyStorage storage = energyCap.orElse(null);
                if (storage != null && storage.getEnergyStored() > 0) {
                    return storage.getEnergyStored();
                }
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> providerClass = Class.forName("com.maxwell.cyber_ware_port.common.capability.CyberwareCapabilityProvider");
            Field capField = providerClass.getField("CYBERWARE_CAPABILITY");
            net.minecraftforge.common.capabilities.Capability<?> cap =
                    (net.minecraftforge.common.capabilities.Capability<?>) capField.get(null);

            var lazyOpt = player.getCapability(cap);
            if (lazyOpt.isPresent()) {
                Object userData = lazyOpt.orElse(null);
                if (userData != null) {
                    if (userData instanceof IEnergyStorage storage) {
                        return storage.getEnergyStored();
                    }

                    for (Method m : userData.getClass().getMethods()) {
                        String name = m.getName().toLowerCase();
                        if ((name.contains("power") || name.contains("energy") || name.contains("stored")) 
                                && m.getParameterCount() == 0 
                                && (m.getReturnType() == int.class || m.getReturnType() == Integer.class || m.getReturnType() == float.class)) {
                            try {
                                Object res = m.invoke(userData);
                                if (res instanceof Number num) {
                                    int val = num.intValue();
                                    if (val > 0) return val;
                                }
                            } catch (Throwable ignored) {}
                        }
                    }

                    for (Field f : userData.getClass().getDeclaredFields()) {
                        String name = f.getName().toLowerCase();
                        if (name.contains("power") || name.contains("energy") || name.contains("stored")) {
                            try {
                                f.setAccessible(true);
                                Object res = f.get(userData);
                                if (res instanceof Number num) {
                                    int val = num.intValue();
                                    if (val > 0) return val;
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        return 0;
    }

    private static void extractCyberwarePower(Player player, int amount) {
        try {
            var energyCap = player.getCapability(ForgeCapabilities.ENERGY);
            if (energyCap.isPresent()) {
                IEnergyStorage storage = energyCap.orElse(null);
                if (storage != null && storage.canExtract()) {
                    storage.extractEnergy(amount, false);
                    return;
                }
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> providerClass = Class.forName("com.maxwell.cyber_ware_port.common.capability.CyberwareCapabilityProvider");
            Field capField = providerClass.getField("CYBERWARE_CAPABILITY");
            net.minecraftforge.common.capabilities.Capability<?> cap =
                    (net.minecraftforge.common.capabilities.Capability<?>) capField.get(null);

            var lazyOpt = player.getCapability(cap);
            if (lazyOpt.isPresent()) {
                Object userData = lazyOpt.orElse(null);
                if (userData != null) {
                    if (userData instanceof IEnergyStorage storage) {
                        storage.extractEnergy(amount, false);
                        return;
                    }

                    for (Method m : userData.getClass().getMethods()) {
                        String name = m.getName().toLowerCase();
                        if ((name.contains("use") || name.contains("extract") || name.contains("consume")) 
                                && (name.contains("power") || name.contains("energy")) 
                                && m.getParameterCount() == 1 && m.getParameterTypes()[0] == int.class) {
                            try {
                                m.invoke(userData, amount);
                                return;
                            } catch (Throwable ignored) {}
                        }
                    }

                    for (Field f : userData.getClass().getDeclaredFields()) {
                        String name = f.getName().toLowerCase();
                        if (name.contains("power") || name.contains("energy") || name.contains("stored")) {
                            try {
                                f.setAccessible(true);
                                Object res = f.get(userData);
                                if (res instanceof Number num) {
                                    int current = num.intValue();
                                    f.setInt(userData, Math.max(0, current - amount));
                                    return;
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}