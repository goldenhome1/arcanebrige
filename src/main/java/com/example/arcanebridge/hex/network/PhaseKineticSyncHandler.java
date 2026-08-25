package com.example.arcanebridge.hex.network;

import com.example.arcanebridge.hex.util.KineticValidationHelper;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class PhaseKineticSyncHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null || server.getTickCount() % 5 != 0) return; // Каждые 5 тиков

        PhaseNetworkManager manager = PhaseNetworkManager.get(server);

        // Обход всех каналов и синхронизация RPM между валами
        for (double channelId = 0; channelId < 1000; channelId++) {
            var channel = manager.getChannel(channelId);
            if (channel != null && channel.transmitter != null && channel.receiver != null) {
                ServerLevel txLevel = server.getLevel(channel.transmitter.dimension());
                ServerLevel rxLevel = server.getLevel(channel.receiver.dimension());

                if (txLevel != null && rxLevel != null && txLevel.isLoaded(channel.transmitter.pos()) && rxLevel.isLoaded(channel.receiver.pos())) {
                    KineticBlockEntity txBlock = KineticValidationHelper.getKineticBlockEntity(txLevel, channel.transmitter.pos());
                    KineticBlockEntity rxBlock = KineticValidationHelper.getKineticBlockEntity(rxLevel, channel.receiver.pos());

                                        if (txBlock != null && rxBlock != null) {
                        float speed = txBlock.getSpeed();
                        if (Math.abs(rxBlock.getSpeed() - speed) > 0.1f) {
                            rxBlock.setSpeed(speed);
                            rxBlock.onSpeedChanged(0);
                        }

                        // Если валы крутятся — генерируем рабочие частицы эфира
                        if (Math.abs(speed) > 0.1f) {
                            txLevel.sendParticles(
                                    net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                                    channel.transmitter.pos().getX() + 0.5,
                                    channel.transmitter.pos().getY() + 0.5,
                                    channel.transmitter.pos().getZ() + 0.5,
                                    2, 0.25, 0.25, 0.25, 0.02
                            );
                            rxLevel.sendParticles(
                                    net.minecraft.core.particles.ParticleTypes.PORTAL,
                                    channel.receiver.pos().getX() + 0.5,
                                    channel.receiver.pos().getY() + 0.5,
                                    channel.receiver.pos().getZ() + 0.5,
                                    3, 0.25, 0.25, 0.25, 0.05
                            );
                        }
                    }
                }
            }
        }
    }
}