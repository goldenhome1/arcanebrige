package com.example.arcanebridge.hex.actions;

import at.petrak.hexcasting.api.casting.OperatorUtils;
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.Mishap;
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock;
import at.petrak.hexcasting.api.misc.MediaConstants;
import com.example.arcanebridge.block.PhaseFluidBlock;
import com.example.arcanebridge.block.entity.PhaseFluidBlockEntity;
import com.example.arcanebridge.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OpPhaseFluidTX implements ConstMediaAction {

    public static final OpPhaseFluidTX INSTANCE = new OpPhaseFluidTX();

    @Override
    public int getArgc() {
        return 2; // Вектор (Позиция), Число (Канал)
    }

    @Override
    public long getMediaCost() {
        return MediaConstants.DUST_UNIT * 2L;
    }

    @NotNull
    @Override
    public List<Iota> execute(@NotNull List<? extends Iota> args, @NotNull CastingEnvironment env) throws Mishap {
        BlockPos pos = OperatorUtils.getBlockPos(args, 0, getArgc());
        int channel = OperatorUtils.getInt(args, 1, getArgc());

        env.assertPosInRange(pos);

        Level level = env.getWorld();
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);

        boolean isValidTarget = state.getBlock() instanceof PhaseFluidBlock
                || state.getBlock() instanceof com.simibubi.create.content.fluids.pipes.FluidPipeBlock
                || state.getBlock() instanceof com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock
                || state.getBlock() instanceof com.simibubi.create.content.fluids.pipes.EncasedPipeBlock
                || (be != null && be.getCapability(ForgeCapabilities.FLUID_HANDLER).isPresent());

        if (!isValidTarget) {
            throw new MishapBadBlock(pos, Component.translatable("hexcasting.mishap.bad_block.pipe"));
        }

        Direction.Axis axis = Direction.Axis.Y;
        if (state.hasProperty(PhaseFluidBlock.AXIS)) {
            axis = state.getValue(PhaseFluidBlock.AXIS);
        } else if (state.hasProperty(BlockStateProperties.AXIS)) {
            axis = state.getValue(BlockStateProperties.AXIS);
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            axis = state.getValue(BlockStateProperties.FACING).getAxis();
        }

        if (!(state.getBlock() instanceof PhaseFluidBlock)) {
            level.setBlock(pos, ModBlocks.PHASE_FLUID_RELAY.get().defaultBlockState().setValue(PhaseFluidBlock.AXIS, axis), Block.UPDATE_ALL);
        }

        be = level.getBlockEntity(pos);
        if (be instanceof PhaseFluidBlockEntity fluidNode) {
            fluidNode.tune(Math.max(1, channel), false); // false = Источник (TX)

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 25, 0.3, 0.3, 0.3, 0.15);
                serverLevel.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.5);
            }
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.8F);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);
        }

        return List.of();
    }
}