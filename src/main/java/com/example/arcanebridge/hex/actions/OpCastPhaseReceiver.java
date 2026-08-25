package com.example.arcanebridge.hex.actions;

import at.petrak.hexcasting.api.casting.castables.Action;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.CastingImage;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
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

        // 1. Проверяем вал Create
        if (!KineticValidationHelper.isKineticBlock(level, targetPos)) {
            newStack.add(new NullIota());
            return buildResult(image, continuation, newStack);
        }

                // 2. Регистрируем вал как Приемник
        PhaseNetworkManager manager = PhaseNetworkManager.get(level.getServer());
        manager.registerReceiver(channelId, level.dimension(), targetPos);

        // Визуальный и звуковой отклик каста
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
        try {
            CastingImage nextImage = (CastingImage) image.getClass().getMethod("withStack", List.class).invoke(image, newStack);
            for (java.lang.reflect.Constructor<?> c : OperationResult.class.getConstructors()) {
                if (c.getParameterCount() == 4) {
                    return (OperationResult) c.newInstance(nextImage, continuation, new ArrayList<>(), null);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}