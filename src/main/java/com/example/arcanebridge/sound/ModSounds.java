package com.example.arcanebridge.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "arcane_bridge");

    public static final RegistryObject<SoundEvent> ARCANE_BREACH =
            SOUND_EVENTS.register("arcane_breach", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("arcane_bridge", "arcane_breach")));
    
    public static final RegistryObject<SoundEvent> SYNDICATE_RAID =
            SOUND_EVENTS.register("syndicate_raid", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("arcane_bridge", "syndicate_raid")));
    
    public static final RegistryObject<SoundEvent> THERMAL_SURGE =
            SOUND_EVENTS.register("thermal_surge", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("arcane_bridge", "thermal_surge")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}