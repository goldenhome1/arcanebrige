package com.example.arcanebridge.hex.actions;

import at.petrak.hexcasting.api.casting.OperatorUtils;
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.DoubleIota;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.NullIota;
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
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OpCastPhaseReceiver implements ConstMediaAction {

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
        Vec3 targetVec = OperatorUtils.getVec3(args, 0, getArgc());
        double channelId = OperatorUtils.getDouble(args, 1, getArgc());
        BlockPos targetPos = BlockPos.containing(targetVec);
        ServerLevel level = env.getWorld();
        BlockState targetState = level.getBlockState(targetPos);

        Block shaftBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create", "shaft"));
        boolean isCreateShaft = shaftBlock != null && targetState.is(shaftBlock);
        boolean isPhaseRelay = targetState.getBlock() instanceof PhaseRelayBlock;

        if (!isCreateShaft && !isPhaseRelay) {
            return List.of(new NullIota());
        }

        if (isCreateShaft) {
            Direction.Axis axis = targetState.hasProperty(PhaseRelayBlock.AXIS)
                    ? targetState.getValue(PhaseRelayBlock.AXIS)
                    : Direction.Axis.Y;
            BlockState newState = ModBlocks.PHASE_RELAY.get().defaultBlockState()
                    .setValue(PhaseRelayBlock.AXIS, axis);
            level.setBlock(targetPos, newState, Block.UPDATE_ALL);
        }

        if (level.getBlockEntity(targetPos) instanceof PhaseRelayBlockEntity relay) {
            relay.tuneChannel(channelId, true);
            level.playSound(null, targetPos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.9F, 1.4F);
        }

        return List.of(new DoubleIota(channelId));
    }

    @NotNull
    @Override
    public ConstMediaAction.CostMediaActionResult executeWithOpCount(@NotNull List<? extends Iota> args, @NotNull CastingEnvironment env) {
        return ConstMediaAction.DefaultImpls.executeWithOpCount(this, args, env);
    }

    @NotNull
    @Override
    public OperationResult operate(@NotNull CastingEnvironment env, @NotNull CastingImage image, @NotNull SpellContinuation continuation) {
        return ConstMediaAction.DefaultImpls.operate(this, env, image, continuation);
    }
}