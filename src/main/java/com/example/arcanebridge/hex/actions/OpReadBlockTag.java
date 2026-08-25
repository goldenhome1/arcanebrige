package com.example.arcanebridge.hex.actions;

import at.petrak.hexcasting.api.casting.castables.Action; // Точное расположение интерфейса Action!
import at.petrak.hexcasting.api.casting.eval.*;
import at.petrak.hexcasting.api.casting.eval.vm.*;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.DoubleIota;
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

public class OpReadBlockTag implements Action {


    @NotNull

    @Override

    public OperationResult operate(@NotNull CastingEnvironment env, @NotNull CastingImage image, @NotNull SpellContinuation continuation) {

        List<Iota> newStack = new ArrayList<>(image.getStack());


        if (newStack.size() < 2) {

            newStack.add(new NullIota());

            return createResult(image, newStack);

        }


        Iota second = newStack.remove(newStack.size() - 1);

        Iota first = newStack.remove(newStack.size() - 1);


        if (!(first instanceof Vec3Iota)) {

            newStack.add(new NullIota());

            return createResult(image, newStack);

        }


        Vec3 position = ((Vec3Iota) first).getVec3();

        BlockPos blockPos = BlockPos.containing(position);


        String tagName = "";

        try {

            tagName = (String) second.getClass().getMethod("getString").invoke(second);

        } catch (Exception e) {

            tagName = second.toString();

        }


        BlockEntity be = env.getWorld().getBlockEntity(blockPos);


        if (be != null) {

            CompoundTag nbt = be.saveWithId();


            if (nbt.contains(tagName)) {

                byte type = nbt.getTagType(tagName);


                if (type >= Tag.TAG_BYTE && type <= Tag.TAG_DOUBLE) {

                    newStack.add(new DoubleIota(nbt.getDouble(tagName)));

                } else {

                    newStack.add(new NullIota());

                }

            } else {

                newStack.add(new NullIota());

            }

        } else {

            newStack.add(new NullIota());

        }


        return createResult(image, newStack);

    }


    private OperationResult createResult(CastingImage image, List<Iota> newStack) {

        CastingImage nextImage = image.copy(

                newStack,

                image.getParenChildren(),

                image.getEscapeNext(),

                image.getOpsConsumed(),

                image.getUserData()

        );

        return new OperationResult(nextImage, new ArrayList<>());

    }

}
