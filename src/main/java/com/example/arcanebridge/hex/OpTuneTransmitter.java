package com.example.arcanebridge.hex;

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.DoubleIota;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.Vec3Iota;
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock;
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota;
import at.petrak.hexcasting.api.misc.MediaConstants;
import com.example.arcanebridge.block.PhaseRelayBlock;
import com.example.arcanebridge.block.entity.PhaseRelayBlockEntity;
import com.example.arcanebridge.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class OpTuneTransmitter implements ConstMediaAction {

        @Override
    public int getArgc() {
        return 2;
    }

    @Override
    public long getMediaCost() {
        return 0;
    }

    @Override
    public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation continuation) {
        return ConstMediaAction.DefaultImpls.operate(this, env, image, continuation);
    }

    @Override
    public List<Iota> execute(List<? extends Iota> args, CastingEnvironment env) {
        Iota vecIota = args.get(0);
        Iota numIota = args.get(1);

        if (!(vecIota instanceof Vec3Iota vIota)) {
            throw new MishapInvalidIota(vecIota, 1, null);
        }
        if (!(numIota instanceof DoubleIota dIota)) {
            throw new MishapInvalidIota(numIota, 0, null);
        }

        Vec3 targetVec = vIota.getVec3();
        env.assertVecInRange(targetVec);

        BlockPos targetPos = BlockPos.containing(targetVec);
        ServerLevel level = env.getWorld();
        BlockState targetState = level.getBlockState(targetPos);

        Block shaftBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create", "shaft"));
        boolean isCreateShaft = shaftBlock != null && targetState.is(shaftBlock);
        boolean isPhaseRelay = targetState.getBlock() instanceof PhaseRelayBlock;

        if (!isCreateShaft && !isPhaseRelay) {
            throw new MishapBadBlock(targetPos, targetState.getBlock().getName());
        }

        long cost = isCreateShaft ? MediaConstants.SHARD_UNIT : MediaConstants.DUST_UNIT;
        env.extractMedia(cost, false);

        double channel = dIota.getDouble();

        if (isCreateShaft) {
            Direction.Axis axis = targetState.hasProperty(PhaseRelayBlock.AXIS)
                    ? targetState.getValue(PhaseRelayBlock.AXIS)
                    : Direction.Axis.Y;

            BlockState newState = ModBlocks.PHASE_RELAY.get().defaultBlockState()
                    .setValue(PhaseRelayBlock.AXIS, axis);
            level.setBlock(targetPos, newState, Block.UPDATE_ALL);
        }

        if (level.getBlockEntity(targetPos) instanceof PhaseRelayBlockEntity relay) {
            relay.tuneChannel(channel, false);
            level.playSound(null, targetPos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.2F);
        }

        return List.of();
    }
}