package com.example.arcanebridge.hex.util;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class KineticValidationHelper {

    /**
     * Проверка: является ли блок кинетическим узлом Create
     */
    public static boolean isKineticBlock(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof KineticBlockEntity;
    }

    /**
     * Получение KineticBlockEntity
     */
    public static KineticBlockEntity getKineticBlockEntity(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof KineticBlockEntity kbe) {
            return kbe;
        }
        return null;
    }
}