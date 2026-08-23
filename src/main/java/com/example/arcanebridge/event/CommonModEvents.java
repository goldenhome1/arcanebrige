package com.example.arcanebridge.event;

import com.example.arcanebridge.entity.ArcaneGuideEntity;
import com.example.arcanebridge.entity.ModEntities;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "arcane_bridge", bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonModEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ARCANE_GUIDE.get(), ArcaneGuideEntity.createAttributes().build());
    }
}