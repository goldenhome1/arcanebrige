package com.example.arcanebridge.hex.actions;

import at.petrak.hexcasting.api.casting.castables.Action;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.DoubleIota;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.NullIota;
import at.petrak.hexcasting.api.casting.iota.Vec3Iota;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class OpReadBlockTag implements ConstMediaAction {

    @Override
    public int getArgc() {
        return 2;
    }

    @Override
    public long getMediaCost() {
        return 0L;
    }

    @NotNull
    @Override
    public List<Iota> execute(@NotNull List<? extends Iota> args, @NotNull CastingEnvironment env) {
        Vec3 position = at.petrak.hexcasting.api.casting.OperatorUtils.getVec3(args, 0, getArgc());
        Iota second = args.get(1);
        BlockPos blockPos = BlockPos.containing(position);

        String tagName = second.toString();
        try {
            tagName = (String) second.getClass().getMethod("getString").invoke(second);
        } catch (Exception ignored) {}

        BlockEntity be = env.getWorld().getBlockEntity(blockPos);
        if (be != null) {
            CompoundTag nbt = be.saveWithId();
            if (nbt.contains(tagName)) {
                byte type = nbt.getTagType(tagName);
                if (type >= Tag.TAG_BYTE && type <= Tag.TAG_DOUBLE) {
                    return List.of(new DoubleIota(nbt.getDouble(tagName)));
                }
            }
        }

        return List.of(new NullIota());
    }

    @NotNull
    @Override
    public OperationResult operate(@NotNull CastingEnvironment env, @NotNull CastingImage image, @NotNull SpellContinuation continuation) {
        return ConstMediaAction.DefaultImpls.operate(this, env, image, continuation);
    }
}