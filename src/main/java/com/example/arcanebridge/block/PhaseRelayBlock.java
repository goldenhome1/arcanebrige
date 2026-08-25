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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class PhaseRelayBlock extends RotatedPillarKineticBlock implements IBE<PhaseRelayBlockEntity> {

    public PhaseRelayBlock(Properties properties) {
        super(properties);
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
                    relay.isReceiver = !relay.isReceiver;
                    relay.notifyUpdate();
                    player.sendSystemMessage(Component.literal("§6[Фазовый Резонатор] §7Режим переключен на: " + 
                            (relay.isReceiver ? "§c[RX: Приемник/Генератор]" : "§f[TX: Передатчик/Сенсор]")));
                } else {
                    player.sendSystemMessage(Component.literal("§6[Фазовый Резонатор] §7Канал: §e" + relay.channelId + 
                            " §7| Режим: " + (relay.isReceiver ? "§cRX" : "§fTX") + 
                            " §7| Текущая скорость: §b" + relay.getSpeed() + " RPM"));
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