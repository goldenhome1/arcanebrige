package com.example.arcanebridge.event;

import com.example.arcanebridge.fluid.PhaseFluidCapabilityProvider;
import com.example.arcanebridge.fluid.PhaseFluidSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class ModCommonEvents {

    private static final ResourceLocation PHASE_FLUID_CAP_KEY = new ResourceLocation("arcane_bridge", "phase_fluid_capability");

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel && serverLevel.dimension() == ServerLevel.OVERWORLD) {
            PhaseFluidSavedData.init(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        event.addCapability(PHASE_FLUID_CAP_KEY, new PhaseFluidCapabilityProvider(event.getObject()));
    }
}