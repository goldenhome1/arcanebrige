package com.example.arcanebridge.hex.actions;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.Mishap;
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock;
import at.petrak.hexcasting.api.casting.OperatorUtils;
import at.petrak.hexcasting.api.misc.MediaConstants;
import com.example.arcanebridge.block.PhaseFluidBlock;
import kotlin.Triple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class OpPhaseFluidClose implements SpellAction {

    public static final OpPhaseFluidClose INSTANCE = new OpPhaseFluidClose();

    @Override
    public int getArgc() {
        return 1; // Вектор (Позиция)
    }

    @Override
    public Triple<RenderedSpell, Long, List<ParticleSpray>> execute(List<? extends Iota> args, CastingEnvironment env) throws Mishap {
        BlockPos pos = OperatorUtils.getBlockPos(args, 0, getArgc());
        env.assertPosInRange(pos);

        Level level = env.getWorld();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof PhaseFluidBlock)) {
            throw new MishapBadBlock(pos, Component.translatable("hexcasting.mishap.bad_block.phase_fluid"));
        }

        return new Triple<>(
                new Spell(pos),
                MediaConstants.DUST_UNIT,
                List.of(ParticleSpray.cloud(Vec3.atCenterOf(pos), 0.8, 15))
        );
    }

    private record Spell(BlockPos pos) implements RenderedSpell {
        @Override
        public void cast(CastingEnvironment env) {
            Level level = env.getWorld();
            BlockState state = level.getBlockState(pos);

            if (state.getBlock() instanceof PhaseFluidBlock) {
                // Возврат в стандартную трубу Create с сохранением сетки соединений
                level.setBlock(pos, com.simibubi.create.AllBlocks.FLUID_PIPE.getDefaultState(), Block.UPDATE_ALL);

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.2, 0.2, 0.2, 0.05);
                    serverLevel.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 0.3, 0.3, 0.3, 0.1);
                }
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.5F);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
            }
        }
    }
}