package com.example.arcanebridge.hex.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.reflect.Method;

public class KineticValidationHelper {

    /**
     * Проверка: является ли блок кинетическим узлом Create
     */
    public static boolean isKineticBlock(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;
        String className = be.getClass().getName();
        return className.contains("create") && (className.contains("Kinetic") || className.contains("Shaft"));
    }

    /**
     * Безопасное чтение скорости вращения RPM
     */
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

    /**
     * Безопасная установка скорости вращения RPM
     */
    public static void setSpeed(BlockEntity be, float speed) {
        if (be == null) return;
        try {
            Method setSpeedMethod = be.getClass().getMethod("setSpeed", float.class);
            setSpeedMethod.invoke(be, speed);

            try {
                Method onSpeedChangedMethod = be.getClass().getMethod("onSpeedChanged", float.class);
                onSpeedChangedMethod.invoke(be, 0.0f);
            } catch (NoSuchMethodException ignored) {
                Method onSpeedChangedNoArg = be.getClass().getMethod("onSpeedChanged");
                onSpeedChangedNoArg.invoke(be);
            }
        } catch (Throwable ignored) {}
    }
}