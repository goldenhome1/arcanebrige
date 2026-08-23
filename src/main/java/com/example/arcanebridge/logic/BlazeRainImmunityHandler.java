package com.example.arcanebridge.logic;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class BlazeRainImmunityHandler {

    @SubscribeEvent
    public static void onBlazeAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Blaze) {
            if (event.getSource().is(DamageTypes.DROWN) || "drown".equalsIgnoreCase(event.getSource().getMsgId())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onBlazeHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Blaze) {
            if (event.getSource().is(DamageTypes.DROWN) || "drown".equalsIgnoreCase(event.getSource().getMsgId())) {
                event.setCanceled(true);
            }
        }
    }
}