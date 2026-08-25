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

    // Хитбоксы вала Create по трем осям
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND) {
            if (level.getBlockEntity(pos) instanceof PhaseRelayBlockEntity relay) {
                if (player.isShiftKeyDown()) {
                    relay.unregisterFromNetwork();
                    relay.isReceiver = !relay.isReceiver;
                    relay.registerInNetwork();
                    relay.updateGeneratedRotation();
                    relay.notifyUpdate();
                    player.sendSystemMessage(Component.literal("§6[Фазовый Резонатор] §7Режим переключен: " + 
                            (relay.isReceiver ? "§c[RX: Приемник/Генератор]" : "§f[TX: Передатчик/Сенсор]")));
                } else {
                    relay.registerInNetwork();
                    player.sendSystemMessage(Component.literal("§6[Фазовый Резонатор] §7Канал: §e" + relay.channelId + 
                            " §7| Режим: " + (relay.isReceiver ? "§cRX" : "§fTX") + 
                            " §7| Скорость: §b" + relay.getSpeed() + " RPM"));
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.SUCCESS;
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