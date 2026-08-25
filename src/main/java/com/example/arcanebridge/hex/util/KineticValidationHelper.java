package com.example.arcanebridge.hex.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

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

    /**
     * Запуск нативного пересчета физики Create для узла-генератора
     */
    public static void updateGeneratedRotation(BlockEntity be) {
        if (be == null) return;
        try {
            Method updateGen = be.getClass().getMethod("updateGeneratedRotation");
            updateGen.invoke(be);
        } catch (Throwable t) {
            try {
                Method detach = be.getClass().getMethod("detachKinetics");
                Method attach = be.getClass().getMethod("attachKinetics");
                detach.invoke(be);
                attach.invoke(be);
            } catch (Throwable ignored) {}
        }
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

    public static float getNetworkCapacity(BlockEntity be) {
        if (be == null) return 0.0f;
        try {
            Method getNet = be.getClass().getMethod("getOrCreateNetwork");
            Object net = getNet.invoke(be);
            if (net != null) {
                Method calcCap = net.getClass().getMethod("calculateCapacity");
                Object res = calcCap.invoke(net);
                if (res instanceof Number num) {
                    return num.floatValue();
                }
            }
        } catch (Throwable ignored) {}
        return 0.0f;
    }
}