package com.example.arcanebridge;

import com.example.arcanebridge.capability.ResonanceProvider;
import com.example.arcanebridge.decipher.DecryptionRegistry;
import com.example.arcanebridge.entity.ModEntities;
import com.example.arcanebridge.hex.ArcaneHexRegistry;
import com.example.arcanebridge.hex.ModHexActions;
import com.example.arcanebridge.item.ModItems;
import com.example.arcanebridge.logic.ResonanceEngine;
import com.example.arcanebridge.network.NetworkHandler;
import com.example.arcanebridge.raid.RaidConfig;
import com.example.arcanebridge.registry.ModBlockEntities;
import com.example.arcanebridge.registry.ModBlocks;
import com.example.arcanebridge.sound.ModSounds;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("arcane_bridge")
public class ArcaneBridge {

    public static final String MODID = "arcane_bridge";

    public ArcaneBridge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

                NetworkHandler.register();
        ModSounds.register(modEventBus);
        ArcaneHexRegistry.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        com.example.arcanebridge.registry.ModHexActions.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        ModHexActions.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
                event.enqueueWork(() -> {
            ResonanceEngine.loadOrCreateConfig();
            RaidConfig.loadOrCreateConfig();
            
                        // Регистрация доступных для расшифровки реликвий
                        DecryptionRegistry.register(
                    ModItems.ANCIENT_SCROLL_PHASE.get(),
                    ModItems.DECIPHERED_SCROLL_PHASE.get(),
                    new ResourceLocation("arcane_bridge", "spells/phase_kinetics"),
                    "decipher_phase_kinetics_success",
                    "Фазовый Резонанс (Кинетика)"
            );

            DecryptionRegistry.register(
                    ModItems.ANCIENT_SCROLL_FLUID.get(),
                    ModItems.DECIPHERED_SCROLL_FLUID.get(),
                    new ResourceLocation("arcane_bridge", "spells/phase_fluidics"),
                    "decipher_fluid_explain",
                    "Фазовый Резонанс (Гидродинамика)"
            );
        });
    }

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(new ResourceLocation("arcane_bridge", "resonance"), new ResonanceProvider());
        }
    }
}