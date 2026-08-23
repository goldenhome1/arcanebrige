package com.example.arcanebridge.client;

import com.example.arcanebridge.network.ClientboundResonanceSyncPacket;
import com.example.arcanebridge.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "arcane_bridge", value = Dist.CLIENT)
public class RaidMusicHandler {

    private static SimpleSoundInstance currentMusicInstance = null;
    private static boolean wasInRaidZone = false;
    private static String lastRaidType = "";

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            stopMusic();
            wasInRaidZone = false;
            lastRaidType = "";
            return;
        }

        boolean inZone = ClientboundResonanceSyncPacket.inRaidZoneStatic;
        String currentType = RiftSkyRenderer.currentRaidType != null ? RiftSkyRenderer.currentRaidType : "arcane_breach";

        // Вход в зону рейда или смена типа рейда в процессе
        if (inZone && (!wasInRaidZone || !currentType.equals(lastRaidType))) {
            playMusic(currentType);
            lastRaidType = currentType;
        } 
        // Выход из зоны рейда
        else if (!inZone && wasInRaidZone) {
            stopMusic();
            lastRaidType = "";
        }

        wasInRaidZone = inZone;
    }

    public static void playMusic(String raidType) {
        Minecraft mc = Minecraft.getInstance();
        stopMusic();

        ResourceLocation soundId;
        switch (raidType.toLowerCase()) {
            case "syndicate_raid":
                soundId = ModSounds.SYNDICATE_RAID.get().getLocation();
                break;
            case "thermal_surge":
                soundId = ModSounds.THERMAL_SURGE.get().getLocation();
                break;
            case "arcane_breach":
            default:
                soundId = ModSounds.ARCANE_BREACH.get().getLocation();
                break;
        }

        try {
            currentMusicInstance = new SimpleSoundInstance(
                    soundId,
                    SoundSource.RECORDS, // Регулируется ползунком проигрывателей/нотных блоков
                    1.0F,
                    1.0F,
                    SoundInstance.createUnseededRandom(),
                    true, // looping: true
                    0,
                    SoundInstance.Attenuation.NONE, // Без затухания от расстояния
                    0.0D, 0.0D, 0.0D,
                    true
            );

            mc.getSoundManager().play(currentMusicInstance);
        } catch (Exception ignored) {}
    }

    public static void stopMusic() {
        Minecraft mc = Minecraft.getInstance();
        if (currentMusicInstance != null) {
            mc.getSoundManager().stop(currentMusicInstance);
            currentMusicInstance = null;
        }
        if (ModSounds.ARCANE_BREACH.isPresent()) mc.getSoundManager().stop(ModSounds.ARCANE_BREACH.get().getLocation(), SoundSource.RECORDS);
        if (ModSounds.SYNDICATE_RAID.isPresent()) mc.getSoundManager().stop(ModSounds.SYNDICATE_RAID.get().getLocation(), SoundSource.RECORDS);
        if (ModSounds.THERMAL_SURGE.isPresent()) mc.getSoundManager().stop(ModSounds.THERMAL_SURGE.get().getLocation(), SoundSource.RECORDS);
    }
}