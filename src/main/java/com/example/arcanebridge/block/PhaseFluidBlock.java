package com.example.arcanebridge.block;

import com.example.arcanebridge.block.entity.PhaseFluidBlockEntity;
import com.example.arcanebridge.registry.ModBlockEntities;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidUtil;

public class PhaseFluidBlock extends Block implements IBE<PhaseFluidBlockEntity>, IWrenchable {

    public PhaseFluidBlock(Properties properties) {
        super(properties.noOcclusion());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhaseFluidBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public Class<PhaseFluidBlockEntity> getBlockEntityClass() {
        return PhaseFluidBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PhaseFluidBlockEntity> getBlockEntityType() {
        return ModBlockEntities.PHASE_FLUID_RELAY.get();
    }
}