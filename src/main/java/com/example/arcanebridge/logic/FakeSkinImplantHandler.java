package com.example.arcanebridge.logic;

import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class FakeSkinImplantHandler {

    private static final String FAKE_SKIN_IMPLANT = "cyber_ware_port:skin_upgrades_fake_skin";

    /**
     * Снижает эффективную дистанцию видимости игрока для мобов на 50%
     */
    @SubscribeEvent
    public static void onLivingVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (CyberwareHelper.hasCyberware(player, FAKE_SKIN_IMPLANT)) {
                event.modifyVisibility(0.50D);
            }
        }
    }

    /**
     * Предотвращает переагривание зомби с дальних дистанций по запаху
     */
    @SubscribeEvent
    public static void onTargetChange(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof Player player && event.getEntity() instanceof Zombie) {
            if (player.isSpectator()) return;

            if (CyberwareHelper.hasCyberware(player, FAKE_SKIN_IMPLANT)) {
                double distanceSq = event.getEntity().distanceToSqr(player);
                // Если зомби пытается сагриться дальше 14 блоков — отменяем захват цели
                if (distanceSq > 196.0D) {
                    event.setCanceled(true);
                }
            }
        }
    }
}