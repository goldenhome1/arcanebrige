package com.example.arcanebridge.hex.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.reflect.Method;

public class KineticValidationHelper {

    public static boolean isKineticBlock(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;
        String className = be.getClass().getName();
        return className.contains("create") && (className.contains("Kinetic") || className.contains("Shaft"));
    }

    /**
     * Поиск первого прилегающего кинетического блока Create вокруг указанных координат
     */
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

            try {
                Method onSpeedChangedMethod = be.getClass().getMethod("onSpeedChanged", float.class);
                onSpeedChangedMethod.invoke(be, 0.0f);
            } catch (Throwable ignored) {
                try {
                    Method onSpeedChangedNoArg = be.getClass().getMethod("onSpeedChanged");
                    onSpeedChangedNoArg.invoke(be);
                } catch (Throwable ignored2) {}
            }

            // Отправка пакета обновления клиенту для визуального вращения
            try {
                Method sendDataMethod = be.getClass().getMethod("sendData");
                sendDataMethod.invoke(be);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }
}