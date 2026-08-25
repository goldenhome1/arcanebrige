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
import com.example.arcanebridge.hex.network.PhaseNetworkManager;
import com.example.arcanebridge.hex.util.KineticValidationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class OpCastPhaseReceiver implements Action {

    @NotNull
    @Override
    public OperationResult operate(@NotNull CastingEnvironment env, @NotNull CastingImage image, @NotNull SpellContinuation continuation) {
        List<Iota> stack = new ArrayList<>();
        try {
            stack = (List<Iota>) image.getClass().getMethod("getStack").invoke(image);
        } catch (Exception ignored) {}

        List<Iota> newStack = new ArrayList<>(stack);
        if (newStack.size() < 2) {
            newStack.add(new NullIota());
            return buildResult(image, continuation, newStack);
        }

        Iota channelIota = newStack.remove(newStack.size() - 1);
        Iota targetIota = newStack.remove(newStack.size() - 1);

        if (!(targetIota instanceof Vec3Iota posIota) || !(channelIota instanceof DoubleIota numIota)) {
            newStack.add(new NullIota());
            return buildResult(image, continuation, newStack);
        }

        Vec3 targetVec = posIota.getVec3();
        BlockPos targetPos = BlockPos.containing(targetVec);
        double channelId = numIota.getDouble();
        ServerLevel level = (ServerLevel) env.getWorld();

        if (!KineticValidationHelper.isKineticBlock(level, targetPos)) {
            newStack.add(new NullIota());
            return buildResult(image, continuation, newStack);
        }

        PhaseNetworkManager manager = PhaseNetworkManager.get(level.getServer());
        manager.registerReceiver(channelId, level.dimension(), targetPos);

        double px = targetPos.getX() + 0.5D;
        double py = targetPos.getY() + 0.5D;
        double pz = targetPos.getZ() + 0.5D;
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, px, py, pz, 24, 0.4, 0.4, 0.4, 0.15);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH, px, py, pz, 12, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, targetPos, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, net.minecraft.sounds.SoundSource.BLOCKS, 0.9F, 1.4F);

        newStack.add(new DoubleIota(channelId));
        return buildResult(image, continuation, newStack);
    }

    private OperationResult buildResult(CastingImage image, SpellContinuation continuation, List<Iota> newStack) {
        CastingImage nextImage = image;
        try {
            for (java.lang.reflect.Method m : image.getClass().getMethods()) {
                if (m.getParameterCount() > 0 && List.class.isAssignableFrom(m.getParameterTypes()[0])) {
                    Object[] args = new Object[m.getParameterCount()];
                    args[0] = newStack;
                    for (int i = 1; i < args.length; i++) {
                        args[i] = null;
                    }
                    nextImage = (CastingImage) m.invoke(image, args);
                    break;
                }
            }
        } catch (Throwable ignored) {}

        for (java.lang.reflect.Constructor<?> c : OperationResult.class.getConstructors()) {
            try {
                c.setAccessible(true);
                Class<?>[] pTypes = c.getParameterTypes();
                if (pTypes.length == 4) {
                    Object[] args = new Object[4];
                    args[0] = nextImage;
                    args[1] = new ArrayList<>();
                    args[2] = continuation;
                    args[3] = null;
                    return (OperationResult) c.newInstance(args);
                }
            } catch (Throwable ignored) {}
        }

        for (java.lang.reflect.Constructor<?> c : OperationResult.class.getConstructors()) {
            try {
                c.setAccessible(true);
                if (c.getParameterCount() >= 2) {
                    Object[] args = new Object[c.getParameterCount()];
                    args[0] = nextImage;
                    args[1] = new ArrayList<>();
                    if (args.length > 2) args[2] = continuation;
                    return (OperationResult) c.newInstance(args);
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }
}