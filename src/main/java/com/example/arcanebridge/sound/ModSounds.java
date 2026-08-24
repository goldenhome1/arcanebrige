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

    public static final RegistryObject<SoundEvent> GUIDE_GREETING_01 =
            SOUND_EVENTS.register("guide.greeting_01", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("arcane_bridge", "guide.greeting_01")));

    public static final RegistryObject<SoundEvent> GUIDE_GREETING_INJURED =
            SOUND_EVENTS.register("guide.greeting_injured", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("arcane_bridge", "guide.greeting_injured")));

    public static final RegistryObject<SoundEvent> GUIDE_RESONANCE_WARNING =
            SOUND_EVENTS.register("guide.resonance_warning", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("arcane_bridge", "guide.resonance_warning")));

    public static final RegistryObject<SoundEvent> GUIDE_ABOUT_MASTER =
            SOUND_EVENTS.register("guide.about_master", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("arcane_bridge", "guide.about_master")));

    public static final RegistryObject<SoundEvent> GUIDE_ABOUT_APPEARANCE =
            SOUND_EVENTS.register("guide.about_appearance", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("arcane_bridge", "guide.about_appearance")));

    public static final RegistryObject<SoundEvent> GUIDE_ABOUT_PHILOSOPHY =
            SOUND_EVENTS.register("guide.about_philosophy", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("arcane_bridge", "guide.about_philosophy")));

    public static final RegistryObject<SoundEvent> GUIDE_ABOUT_HELP =
            SOUND_EVENTS.register("guide.about_help", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("arcane_bridge", "guide.about_help")));

    public static final RegistryObject<SoundEvent> GUIDE_PROGRESSION_NETHER =
            SOUND_EVENTS.register("guide.progression_nether", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("arcane_bridge", "guide.progression_nether")));

    public static final RegistryObject<SoundEvent> GUIDE_BOSS_HINTS =
            SOUND_EVENTS.register("guide.boss_hints", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("arcane_bridge", "guide.boss_hints")));

    public static final RegistryObject<SoundEvent> GUIDE_TACTICS_WROUGHTNAUT =
            SOUND_EVENTS.register("guide.tactics_wroughtnaut", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("arcane_bridge", "guide.tactics_wroughtnaut")));

    public static final RegistryObject<SoundEvent> GUIDE_TACTICS_NAMELESS =
            SOUND_EVENTS.register("guide.tactics_nameless", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("arcane_bridge", "guide.tactics_nameless")));

    public static final RegistryObject<SoundEvent> GUIDE_ASSEMBLY_TIPS =
            SOUND_EVENTS.register("guide.assembly_tips", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("arcane_bridge", "guide.assembly_tips")));

    public static final RegistryObject<SoundEvent> GUIDE_LOCATOR_MENU =
            SOUND_EVENTS.register("guide.locator_menu", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("arcane_bridge", "guide.locator_menu")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}