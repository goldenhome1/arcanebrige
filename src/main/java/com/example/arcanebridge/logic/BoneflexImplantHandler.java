package com.example.arcanebridge.logic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class BoneflexImplantHandler {

    private static final String BONEFLEX_IMPLANT = "cyber_ware_port:bone_upgrades_boneflex";

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.isSpectator()) return;

        DamageSource source = event.getSource();
        String msgId = source.getMsgId().toLowerCase();

        // Проверяем кинетический урон от прессов и жерновов Create
        if (msgId.contains("crush") || msgId.contains("mechanical_press") || msgId.contains("create.")) {
            if (CyberwareHelper.hasCyberware(player, BONEFLEX_IMPLANT)) {
                // Полностью поглощаем урон
                event.setCanceled(true);

                if (player.level() instanceof ServerLevel level) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4F, 1.6F);
                    level.sendParticles(ParticleTypes.CRIT,
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            5, 0.2, 0.3, 0.2, 0.1);
                }
            }
        }
    }
}