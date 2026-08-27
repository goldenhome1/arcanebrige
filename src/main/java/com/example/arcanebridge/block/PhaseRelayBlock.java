package com.example.arcanebridge.block;

import com.example.arcanebridge.block.entity.PhaseRelayBlockEntity;
import com.example.arcanebridge.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PhaseRelayBlock extends RotatedPillarKineticBlock implements IBE<PhaseRelayBlockEntity> {

    protected static final VoxelShape X_AXIS_AABB = Block.box(0, 6, 6, 16, 10, 10);
    protected static final VoxelShape Y_AXIS_AABB = Block.box(6, 0, 6, 10, 16, 10);
    protected static final VoxelShape Z_AXIS_AABB = Block.box(6, 6, 0, 10, 10, 16);

    public PhaseRelayBlock(Properties properties) {
        super(properties.noOcclusion());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> X_AXIS_AABB;
            case Z -> Z_AXIS_AABB;
            case Y -> Y_AXIS_AABB;
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhaseRelayBlockEntity(pos, state);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

        @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public Class<PhaseRelayBlockEntity> getBlockEntityClass() {
        return PhaseRelayBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PhaseRelayBlockEntity> getBlockEntityType() {
        return ModBlockEntities.PHASE_RELAY.get();
    }
}