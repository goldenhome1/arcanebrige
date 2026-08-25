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

public class OpCastPhaseTransmitter implements ConstMediaAction {

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
        Vec3 targetVec = at.petrak.hexcasting.api.casting.OperatorUtils.getVec3(args, 0, getArgc());
        double channelId = at.petrak.hexcasting.api.casting.OperatorUtils.getDouble(args, 1, getArgc());
        BlockPos targetPos = BlockPos.containing(targetVec);
        ServerLevel level = env.getWorld();

        if (!KineticValidationHelper.isKineticBlock(level, targetPos)) {
            return List.of(new NullIota());
        }

        PhaseNetworkManager manager = PhaseNetworkManager.get(level.getServer());
        manager.registerTransmitter(channelId, level.dimension(), targetPos);

        double px = targetPos.getX() + 0.5D;
        double py = targetPos.getY() + 0.5D;
        double pz = targetPos.getZ() + 0.5D;
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, px, py, pz, 16, 0.35, 0.35, 0.35, 0.1);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, px, py, pz, 20, 0.4, 0.4, 0.4, 0.2);
        level.playSound(null, targetPos, net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.6F);

        return List.of(new DoubleIota(channelId));
    }

    @NotNull
    @Override
    public OperationResult operate(@NotNull CastingEnvironment env, @NotNull CastingImage image, @NotNull SpellContinuation continuation) {
        return ConstMediaAction.DefaultImpls.operate(this, env, image, continuation);
    }
}