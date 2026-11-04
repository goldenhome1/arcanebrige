package com.example.arcanebridge.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Function;

public class CgsEmfCompat {

    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) return;

        try {
            Class<?> emfApi = Class.forName("traben.entity_model_features.EMFAnimationApi");
            Method regVanilla = emfApi.getMethod("registerVanillaModelCondition", Function.class);
            Method regPause = emfApi.getMethod("registerPauseCondition", Function.class);
            Function<Object, Boolean> gunCondition = emfEntity -> {
                try {
                    if (emfEntity != null) {
                        try {
                            Method getType = emfEntity.getClass().getMethod("etf$getType");
                            Object entityType = getType.invoke(emfEntity);
                            if (entityType != net.minecraft.world.entity.EntityType.PLAYER) {
                                return false;
                            }
                        } catch (Throwable ignored) {
                        }

                        try {
                            Method getHandItems = emfEntity.getClass().getMethod("etf$getHandItems");
                            Object items = getHandItems.invoke(emfEntity);
                            if (items instanceof Iterable<?> iterable) {
                                for (Object obj : iterable) {
                                    if (obj instanceof ItemStack stack && isCgsWeapon(stack)) {
                                        return true;
                                    }
                                }
                            }
                        } catch (Throwable ignored) {
                        }

                        try {
                            Method getUuid = emfEntity.getClass().getMethod("etf$getUuid");
                            UUID uuid = (UUID) getUuid.invoke(emfEntity);
                            Minecraft mc = Minecraft.getInstance();
                            if (uuid != null && mc != null && mc.level != null) {
                                Player player = mc.level.getPlayerByUUID(uuid);
                                if (player != null) {
                                    if (isCgsWeapon(player.getMainHandItem()) || isCgsWeapon(player.getOffhandItem())) {
                                        return true;
                                    }
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    } else {
                        // If emfEntity is null (often in first person hand rendering checks), check local player
                        Minecraft mc = Minecraft.getInstance();
                        if (mc != null && mc.player != null) {
                            if (isCgsWeapon(mc.player.getMainHandItem()) || isCgsWeapon(mc.player.getOffhandItem())) {
                                return true;
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
                return false;
            };

            regVanilla.invoke(null, gunCondition);
            regPause.invoke(null, gunCondition);

            initialized = true;
            System.out.println("[ArcaneBridge] Successfully registered CGS/NTGL exception handler for EMF!");
        } catch (ClassNotFoundException e) {
            // EMF is not installed
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static boolean isCgsWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        try {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id != null) {
                String ns = id.getNamespace();
                if ("cgs".equals(ns) || "ntgl".equals(ns)) {
                    return true;
                }
            }
            String className = stack.getItem().getClass().getName();
            return className.contains("cgs") || className.contains("ntgl");
        } catch (Throwable ignored) {
            return false;
        }
    }
}
