package com.example.arcanebridge.logic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class PlateletsImplantHandler {

    private static final String PLATELETS_IMPLANT = "cyber_ware_port:heart_upgrades_platelets";
    private static final ResourceLocation BLEEDING_EFFECT_ID = new ResourceLocation("majruszsdifficulty", "bleeding");

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.isSpectator()) return;

        MobEffect effect = event.getEffectInstance().getEffect();
        ResourceLocation effectKey = BuiltInRegistries.MOB_EFFECT.getKey(effect);

        if (effectKey != null && (effectKey.equals(BLEEDING_EFFECT_ID) || effectKey.getPath().contains("bleeding"))) {
            if (CyberwareHelper.hasCyberware(player, PLATELETS_IMPLANT)) {
                // Блокируем наложение эффекта кровотечения
                event.setResult(Event.Result.DENY);

                if (player.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.HEART,
                            player.getX(), player.getY() + 1.2, player.getZ(),
                            2, 0.15, 0.15, 0.15, 0.02);
                }
            }
        }
    }
}