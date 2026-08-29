package com.example.arcanebridge.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MobArchetypes.MODID)
public class BioMutantAuraTicker {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || entity instanceof Player) return;
        if (entity.tickCount % 4 != 0) return; // Тикаем раз в 4 такта для оптимизации TPS

        CompoundTag data = entity.getPersistentData();
        if (!data.contains(MobArchetypes.NBT_ARCHETYPE)) return;

                // Проверяем, активен ли в данный момент слой био-мутанта
        if (data.getBoolean(MobArchetypes.NBT_ALL_SHIELDS_BROKEN)) return;

        net.minecraft.nbt.ListTag layers = data.getList(MobArchetypes.NBT_SHIELD_LAYERS, net.minecraft.nbt.Tag.TAG_COMPOUND);
        int currentIndex = data.getInt(MobArchetypes.NBT_CURRENT_LAYER_INDEX);

        boolean hasActiveBioLayer = false;
        if (!layers.isEmpty() && currentIndex < layers.size()) {
            net.minecraft.nbt.CompoundTag activeLayer = layers.getCompound(currentIndex);
            if (MobArchetypes.Type.BIO.name().equals(activeLayer.getString("Type")) && activeLayer.getFloat("HP") > 0) {
                hasActiveBioLayer = true;
            }
        }

        if (hasActiveBioLayer) {

            ServerLevel level = (ServerLevel) entity.level();
            double time = entity.tickCount * 0.15;
            double radius = entity.getBbWidth() + 0.4;
            double yOffset = entity.getBbHeight() * 0.45;

            // Спавн 2 точек орбитального вихря спор
            for (int i = 0; i < 2; i++) {
                double angle = time + (i * Math.PI);
                double px = entity.getX() + Math.cos(angle) * radius;
                double pz = entity.getZ() + Math.sin(angle) * radius;
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, px, entity.getY() + yOffset, pz, 1, 0.0, 0.02, 0.0, 0.0);
            }
        }
    }
}