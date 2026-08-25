package com.example.arcanebridge.hex.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class KineticValidationHelper {

    public static boolean isKineticBlock(Level level, BlockPos pos) {
        if (level == null || pos == null) return false;
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;
        String className = be.getClass().getName();
        return className.contains("create") && (className.contains("Kinetic") || className.contains("Shaft"));
    }

    public static BlockPos findAdjacentKineticPos(Level level, BlockPos centerPos) {
        for (Direction dir : Direction.values()) {
            BlockPos checkPos = centerPos.relative(dir);
            if (isKineticBlock(level, checkPos)) {
                return checkPos;
            }
        }
        return null;
    }

    public static float getSpeed(BlockEntity be) {
        if (be == null) return 0.0f;
        try {
            Method m = be.getClass().getMethod("getSpeed");
            Object res = m.invoke(be);
            if (res instanceof Number num) {
                return num.floatValue();
            }
        } catch (Throwable ignored) {}
        return 0.0f;
    }

    public static void setSpeed(BlockEntity be, float speed) {
        if (be == null) return;
        try {
            Method setSpeedMethod = be.getClass().getMethod("setSpeed", float.class);
            setSpeedMethod.invoke(be, speed);
        } catch (Throwable ignored) {}
    }

    public static void setBlockEntitySource(BlockEntity be, BlockPos sourcePos) {
        if (be == null) return;
        Class<?> clazz = be.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Field sourceField = clazz.getDeclaredField("source");
                sourceField.setAccessible(true);
                sourceField.set(be, sourcePos);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Throwable ignored) {
                break;
            }
        }
    }

    public static void invokeRotationPropagator(Level level, BlockPos pos, BlockEntity be, boolean added) {
        if (level == null || pos == null || be == null) return;
        try {
            Class<?> rpClass = Class.forName("com.simibubi.create.content.kinetics.RotationPropagator");
            Class<?> kbeClass = Class.forName("com.simibubi.create.content.kinetics.base.KineticBlockEntity");
            String methodName = added ? "handleAdded" : "handleRemoved";
            Method m = rpClass.getMethod(methodName, Level.class, BlockPos.class, kbeClass);
            m.invoke(null, level, pos, be);
        } catch (Throwable ignored) {}
    }

    /**
     * Превращает вал-приемник в активный генератор Create и перестраивает граф физики
     */
    public static void applyPhaseSource(Level level, BlockPos pos, float speed) {
        if (level == null || pos == null) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || !isKineticBlock(level, pos)) return;

        try {
            // 1. Снимаем старый граф кинетики
            invokeRotationPropagator(level, pos, be, false);

            if (Math.abs(speed) > 0.01f) {
                // 2. Объявляем блок источником
                setBlockEntitySource(be, pos);
                setSpeed(be, speed);

                // 3. Запускаем пересчет Create для шестеренок, коробок передач и станков
                invokeRotationPropagator(level, pos, be, true);
            } else {
                setBlockEntitySource(be, null);
                setSpeed(be, 0.0f);
                invokeRotationPropagator(level, pos, be, true);
            }

            // 4. Синхронизируем клиент для визуального вращения
            try {
                Method sendDataMethod = be.getClass().getMethod("sendData");
                sendDataMethod.invoke(be);
            } catch (Throwable ignored) {}

            be.setChanged();
        } catch (Throwable ignored) {}
    }

    public static float getNetworkStress(BlockEntity be) {
        if (be == null) return 0.0f;
        try {
            Method getNet = be.getClass().getMethod("getOrCreateNetwork");
            Object net = getNet.invoke(be);
            if (net != null) {
                Method calcStress = net.getClass().getMethod("calculateStress");
                Object res = calcStress.invoke(net);
                if (res instanceof Number num) {
                    return num.floatValue();
                }
            }
        } catch (Throwable ignored) {}
        return 0.0f;
    }

    public static void updateNetwork(BlockEntity be) {
        if (be == null) return;
        try {
            Method updateNet = be.getClass().getMethod("updateNetwork");
            updateNet.invoke(be);
        } catch (Throwable ignored) {}
    }
}