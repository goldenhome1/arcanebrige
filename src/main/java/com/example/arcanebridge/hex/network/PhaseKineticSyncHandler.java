package com.example.arcanebridge.hex.network;

import com.example.arcanebridge.hex.util.KineticValidationHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class PhaseKineticSyncHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null || server.getTickCount() % 5 != 0) return;

        PhaseNetworkManager manager = PhaseNetworkManager.get(server);

        for (double channelId = 0; channelId < 1000; channelId++) {
            var channel = manager.getChannel(channelId);
            if (channel != null && channel.transmitter != null && channel.receiver != null) {
                ServerLevel txLevel = server.getLevel(channel.transmitter.dimension);
                ServerLevel rxLevel = server.getLevel(channel.receiver.dimension);

                if (txLevel != null && rxLevel != null && txLevel.isLoaded(channel.transmitter.pos) && rxLevel.isLoaded(channel.receiver.pos)) {
                    BlockEntity txBe = txLevel.getBlockEntity(channel.transmitter.pos);
                    BlockEntity rxBe = rxLevel.getBlockEntity(channel.receiver.pos);

                    if (txBe != null && rxBe != null) {
                        float txSpeed = KineticValidationHelper.getSpeed(txBe);

                        // 1. При изменении скорости источника передаем вращение по всему графу Create
                        if (Math.abs(txSpeed - channel.currentSpeed) > 0.05f) {
                            channel.currentSpeed = txSpeed;
                            KineticValidationHelper.applyPhaseSource(rxLevel, channel.receiver.pos, txSpeed);
                        }

                        // 2. Считываем суммарный стресс сети станков на RX и пробрасываем на TX
                        float currentRxStress = KineticValidationHelper.getNetworkStress(rxBe);
                        if (Math.abs(currentRxStress - channel.rxStress) > 0.1f) {
                            channel.rxStress = currentRxStress;
                            KineticValidationHelper.updateNetwork(txBe);
                        }

                        // 3. Эфирные частицы
                        if (Math.abs(txSpeed) > 0.1f && server.getTickCount() % 20 == 0) {
                            txLevel.sendParticles(
                                    net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                                    channel.transmitter.pos.getX() + 0.5,
                                    channel.transmitter.pos.getY() + 0.5,
                                    channel.transmitter.pos.getZ() + 0.5,
                                    3, 0.2, 0.2, 0.2, 0.05
                            );
                            rxLevel.sendParticles(
                                    net.minecraft.core.particles.ParticleTypes.PORTAL,
                                    channel.receiver.pos.getX() + 0.5,
                                    channel.receiver.pos.getY() + 0.5,
                                    channel.receiver.pos.getZ() + 0.5,
                                    4, 0.2, 0.2, 0.2, 0.1
                            );
                        }
                    }
                }
            }
        }
    }
}