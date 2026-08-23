package com.example.arcanebridge.logic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class CombatShockEngine {

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        CompoundTag pData = player.getPersistentData();

        String damageType = source.getMsgId();
        String attackerId = attacker != null && ForgeRegistries.ENTITY_TYPES.getKey(attacker.getType()) != null
                ? ForgeRegistries.ENTITY_TYPES.getKey(attacker.getType()).toString()
                : "";

        // =========================================================================
        // ⚙️ МЕХАНИЧЕСКИЙ СИГНАЛ (Кинетика / ЭМИ)
        // =========================================================================
        boolean isMechDamage = damageType.equals("lightningBolt") || damageType.equals("explosion") || 
                               damageType.equals("anvil") || damageType.equals("fall") || 
                               damageType.equals("flyIntoWall") || damageType.equals("cramming");

        boolean isMechMob = attackerId.contains("alexscaves:magnetron") || attackerId.contains("notor") || 
                            attackerId.contains("teletor") || attackerId.contains("boundroid") ||
                            attackerId.contains("versatiledigger") || attackerId.contains("cyber_zombie") || 
                            attackerId.contains("cyber_thug") || attackerId.contains("mechasent") || 
                            attackerId.contains("wroughtnaut") || attackerId.contains("netherite_monstrosity") ||
                            attackerId.contains("ender_golem") || attackerId.contains("kobolediator");

        if (isMechDamage || isMechMob) {
            // Взводим 3-секундный индикатор тревоги на HUD
            pData.putInt("mech_shock_timer", 60);

            // Легкий визуальный отклик (искры) без оглушения игрока
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.5f, 1.5f);
        }

        // =========================================================================
        // 🔮 АРКАННЫЙ СИГНАЛ (Магия / Иссушение)
        // =========================================================================
        boolean isArcaneDamage = damageType.equals("magic") || damageType.equals("indirect_magic") || 
                                 damageType.equals("wither") || damageType.equals("dragon_breath") ||
                                 damageType.equals("witherSkull");

        boolean isArcaneMob = attackerId.contains("wilden") || attackerId.contains("harbinger") || 
                              attackerId.contains("relic_annihilator") || attackerId.contains("corundum_guardian") ||
                              attackerId.contains("ender_guardian") || attackerId.contains("wadjet") ||
                              attackerId.contains("the_leviathan") || attackerId.contains("nameless_guardian") ||
                              attackerId.contains("immortal");

        if (isArcaneDamage || isArcaneMob) {
            pData.putInt("arcane_shock_timer", 60);

            level.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.6f, 1.6f);
        }

        // =========================================================================
        // 🔥 СТИХИЙНЫЙ СИГНАЛ (Огонь / Мороз / Кипение)
        // =========================================================================
        boolean isEleDamage = damageType.equals("on_fire") || damageType.equals("in_fire") || 
                              damageType.equals("lava") || damageType.equals("freeze") || 
                              damageType.equals("drown") || damageType.equals("hotFloor") ||
                              damageType.equals("sonic_boom");

        boolean isEleMob = attackerId.contains("ignis") || attackerId.contains("ignited_revenant") ||
                           attackerId.contains("nucleeper") || attackerId.contains("radgill") ||
                           attackerId.contains("acid_rain_stalker") || attackerId.contains("frostmaw") || 
                           attackerId.contains("umvuthi");

        if (isEleDamage || isEleMob) {
            pData.putInt("ele_shock_timer", 60);

            if (player.isInWaterRainOrBubble()) {
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.2, 0.3, 0.2, 0.02);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.2f);
            }
        }
    }
}