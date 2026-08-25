package com.example.arcanebridge.hex;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
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

public class OpTuneReceiver implements SpellAction {

    @Override
    public int getArgc() {
        return 2;
    }

    @Override
    public SpellAction.Result execute(List<? extends Iota> args, CastingEnvironment ctx) {
        if (!(args.get(0) instanceof Vec3Iota vecIota)) {
            throw new MishapInvalidIota(args.get(0), 1, null);
        }
        if (!(args.get(1) instanceof DoubleIota numIota)) {
            throw new MishapInvalidIota(args.get(1), 0, null);
        }

        Vec3 targetVec = vecIota.getVec3();
        ctx.assertVecInRange(targetVec);

        BlockPos targetPos = BlockPos.containing(targetVec);
        ServerLevel level = ctx.getWorld();
        BlockState targetState = level.getBlockState(targetPos);

        Block shaftBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create", "shaft"));
        boolean isCreateShaft = shaftBlock != null && targetState.is(shaftBlock);
        boolean isPhaseRelay = targetState.getBlock() instanceof PhaseRelayBlock;

        if (!isCreateShaft && !isPhaseRelay) {
            throw new MishapBadBlock(targetPos, targetState.getBlock().getName());
        }

        double channel = numIota.getDouble();
        long cost = isCreateShaft ? MediaConstants.SHARD_UNIT : MediaConstants.DUST_UNIT;

        return new SpellAction.Result(
                new Spell(targetPos, channel, isCreateShaft, targetState),
                cost,
                List.of(ParticleSpray.cloud(Vec3.atCenterOf(targetPos), 1.5D, 20))
        );
    }

    private record Spell(BlockPos pos, double channel, boolean transform, BlockState oldState) implements RenderedSpell {
        @Override
        public void cast(CastingEnvironment ctx) {
            ServerLevel level = ctx.getWorld();

            if (transform) {
                Direction.Axis axis = oldState.hasProperty(PhaseRelayBlock.AXIS)
                        ? oldState.getValue(PhaseRelayBlock.AXIS)
                        : Direction.Axis.Y;

                BlockState newState = ModBlocks.PHASE_RELAY.get().defaultBlockState()
                        .setValue(PhaseRelayBlock.AXIS, axis);
                level.setBlock(pos, newState, Block.UPDATE_ALL);
            }

            if (level.getBlockEntity(pos) instanceof PhaseRelayBlockEntity relay) {
                relay.tuneChannel(channel, true);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.8F);
            }
        }

        @Override
        public void cast(CastingEnvironment ctx, CastingImage image) {
            cast(ctx);
        }
    }
}