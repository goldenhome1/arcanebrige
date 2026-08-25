package com.example.arcanebridge.hex.network;

import com.example.arcanebridge.hex.util.KineticValidationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class PhaseWoolTestHandler {

    private static final double TEST_CHANNEL = 1.0D;

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        BlockPos clickedPos = event.getPos();
        BlockState state = level.getBlockState(clickedPos);
        Player player = event.getEntity();
        ServerLevel serverLevel = (ServerLevel) level;

        if (state.is(Blocks.WHITE_WOOL)) {
            BlockPos kineticPos = KineticValidationHelper.findAdjacentKineticPos(level, clickedPos);
            if (kineticPos != null) {
                BlockEntity be = level.getBlockEntity(kineticPos);
                float speed = KineticValidationHelper.getSpeed(be);

                PhaseNetworkManager manager = PhaseNetworkManager.get(serverLevel.getServer());
                manager.registerTransmitter(TEST_CHANNEL, level.dimension(), kineticPos);

                var channel = manager.getChannel(TEST_CHANNEL);
                if (channel != null) {
                    channel.currentSpeed = speed;
                    if (channel.receiver != null) {
                        ServerLevel rxLevel = serverLevel.getServer().getLevel(channel.receiver.dimension);
                        if (rxLevel != null && rxLevel.isLoaded(channel.receiver.pos)) {
                            KineticValidationHelper.updateGeneratedRotation(rxLevel.getBlockEntity(channel.receiver.pos));
                        }
                    }
                }

                player.sendSystemMessage(Component.literal("§f[TX: Белая Шерсть] §aПривязана к валу: §e" + kineticPos.toShortString() + " §7(Скорость: §b" + speed + " RPM§7)"));
                serverLevel.playSound(null, clickedPos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 1.8F);
                event.setCanceled(true);
            } else {
                player.sendSystemMessage(Component.literal("§f[TX: Белая Шерсть] §cРядом не найден вал Create!"));
            }
        } else if (state.is(Blocks.RED_WOOL)) {
            BlockPos kineticPos = KineticValidationHelper.findAdjacentKineticPos(level, clickedPos);
            if (kineticPos != null) {
                PhaseNetworkManager manager = PhaseNetworkManager.get(serverLevel.getServer());
                manager.registerReceiver(TEST_CHANNEL, level.dimension(), kineticPos);

                // Запуск генерации вращения на стороне приемника
                KineticValidationHelper.updateGeneratedRotation(serverLevel.getBlockEntity(kineticPos));

                player.sendSystemMessage(Component.literal("§c[RX: Красная Шерсть] §aПривязана к валу: §e" + kineticPos.toShortString() + " §7на Канале §61.0"));
                serverLevel.playSound(null, clickedPos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.9F, 1.4F);
                event.setCanceled(true);
            } else {
                player.sendSystemMessage(Component.literal("§c[RX: Красная Шерсть] §cРядом не найден вал Create!"));
            }
        }
    }
}