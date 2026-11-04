package com.example.arcanebridge.combat;

import com.example.arcanebridge.network.ClientboundSyncShieldPacket;
import com.example.arcanebridge.network.NetworkHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = MobArchetypes.MODID)
public class ArchetypeDamageHandler {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || target instanceof Player || !target.isAlive()) return;

        CompoundTag data = target.getPersistentData();
        if (data.getBoolean(MobArchetypes.NBT_ALL_SHIELDS_BROKEN)) return;
        if (!data.contains(MobArchetypes.NBT_SHIELD_LAYERS, Tag.TAG_LIST)) return;

        ListTag layers = data.getList(MobArchetypes.NBT_SHIELD_LAYERS, Tag.TAG_COMPOUND);
        int currentIndex = data.getInt(MobArchetypes.NBT_CURRENT_LAYER_INDEX);
        if (layers.isEmpty() || currentIndex >= layers.size()) {
            data.putBoolean(MobArchetypes.NBT_ALL_SHIELDS_BROKEN, true);
            syncShieldToTracking(target);
            return;
        }

        float incomingDamage = event.getAmount();
        if (incomingDamage <= 0.0F) return;

        CompoundTag activeLayer = layers.getCompound(currentIndex);
        MobArchetypes.Type archetype;
        try {
            archetype = MobArchetypes.Type.valueOf(activeLayer.getString("Type"));
        } catch (Exception e) {
            return;
        }

        DamageSource source = event.getSource();
        MobArchetypes.AttackCategory category = classifyAttack(target, source);
        boolean isMatchingKey = isProfileMatch(archetype, category);

        // Профильное оружие наносит 100% урона по щиту, непрофильное штрафуется резистом (20%)
        float effectiveShieldDamage = isMatchingKey ? incomingDamage : (incomingDamage * MobArchetypes.SHIELD_DAMAGE_REDUCTION);

        ServerLevel level = (ServerLevel) target.level();
        float damageToProcess = effectiveShieldDamage;
        float totalAbsorbedByShield = 0.0F;

        // Поглощение урона текущим и последующими слоями
        while (damageToProcess > 0.0F && currentIndex < layers.size()) {
            CompoundTag currentLayer = layers.getCompound(currentIndex);
            float layerHp = currentLayer.getFloat("HP");

            if (damageToProcess <= layerHp) {
                currentLayer.putFloat("HP", layerHp - damageToProcess);
                totalAbsorbedByShield += damageToProcess;
                damageToProcess = 0.0F;
                triggerShieldHitEffects(level, target, archetype, isMatchingKey);
            } else {
                totalAbsorbedByShield += layerHp;
                damageToProcess -= layerHp;
                currentLayer.putFloat("HP", 0.0F);

                triggerShieldBreakEffects(level, target, archetype);
                currentIndex++;

                if (currentIndex < layers.size()) {
                    // Звук переключения на внутренний слой
                    level.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 0.9F, 1.3F);
                    try {
                        archetype = MobArchetypes.Type.valueOf(layers.getCompound(currentIndex).getString("Type"));
                    } catch (Exception ignored) {}
                }
            }
        }

        // Проверка полного снятия защиты
        if (currentIndex >= layers.size()) {
            data.putBoolean(MobArchetypes.NBT_ALL_SHIELDS_BROKEN, true);
            data.putBoolean(MobArchetypes.NBT_SHIELD_BROKEN, true);
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.3F, 0.85F);
        }

        data.putInt(MobArchetypes.NBT_CURRENT_LAYER_INDEX, currentIndex);

        // 20% урона пробивает сквозь щит в здоровье моба + остаточный урон после полного снятия щитов
        float finalBodyDamage = (totalAbsorbedByShield * 0.20F) + damageToProcess;
        event.setAmount(finalBodyDamage);

        // Мгновенная синхронизация полосы со всеми клиентами
        syncShieldToTracking(target);
    }

    public static void syncShieldToTracking(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        CompoundTag syncTag = new CompoundTag();
        if (data.contains(MobArchetypes.NBT_SHIELD_LAYERS)) {
            syncTag.put(MobArchetypes.NBT_SHIELD_LAYERS, data.getList(MobArchetypes.NBT_SHIELD_LAYERS, Tag.TAG_COMPOUND));
        }
        syncTag.putInt(MobArchetypes.NBT_CURRENT_LAYER_INDEX, data.getInt(MobArchetypes.NBT_CURRENT_LAYER_INDEX));
        syncTag.putBoolean(MobArchetypes.NBT_ALL_SHIELDS_BROKEN, data.getBoolean(MobArchetypes.NBT_ALL_SHIELDS_BROKEN));

        NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                new ClientboundSyncShieldPacket(entity.getId(), syncTag));
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof LivingEntity living && event.getEntity() instanceof ServerPlayer player) {
            CompoundTag data = living.getPersistentData();
            if (data.contains(MobArchetypes.NBT_SHIELD_LAYERS)) {
                CompoundTag syncTag = new CompoundTag();
                syncTag.put(MobArchetypes.NBT_SHIELD_LAYERS, data.getList(MobArchetypes.NBT_SHIELD_LAYERS, Tag.TAG_COMPOUND));
                syncTag.putInt(MobArchetypes.NBT_CURRENT_LAYER_INDEX, data.getInt(MobArchetypes.NBT_CURRENT_LAYER_INDEX));
                syncTag.putBoolean(MobArchetypes.NBT_ALL_SHIELDS_BROKEN, data.getBoolean(MobArchetypes.NBT_ALL_SHIELDS_BROKEN));
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new ClientboundSyncShieldPacket(living.getId(), syncTag));
            }
        }
    }

    private static MobArchetypes.AttackCategory classifyAttack(LivingEntity target, DamageSource source) {
        String damageTypeId = "";
        try {
            ResourceLocation key = target.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getKey(source.type());
            if (key != null) damageTypeId = key.toString();
        } catch (Exception ignored) {}

        String msgId = source.getMsgId();
        Entity directEntity = source.getDirectEntity();

        if (damageTypeId.contains("ars_nouveau") || damageTypeId.contains("hexcasting") || damageTypeId.contains("magic")
                || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            return MobArchetypes.AttackCategory.MAGIC_SPELL;
        }
        if (directEntity != null) {
            ResourceLocation entId = ForgeRegistries.ENTITY_TYPES.getKey(directEntity.getType());
            if (entId != null && (entId.getNamespace().equals("ars_nouveau") || entId.getNamespace().equals("hexcasting"))) {
                return MobArchetypes.AttackCategory.MAGIC_SPELL;
            }
        }

        if (damageTypeId.contains("ntgl") || damageTypeId.contains("cgs") || damageTypeId.contains("createbigcannons")
                || damageTypeId.contains("machine_gun") || damageTypeId.contains("cannon") || damageTypeId.contains("bullet")
                || msgId.contains("bullet") || msgId.contains("cgs") || msgId.contains("cannon") || msgId.contains("machine_gun_fire")) {
            return MobArchetypes.AttackCategory.CGS_FIREARM;
        }
        if (directEntity instanceof Projectile && !(directEntity instanceof AbstractArrow)) {
            ResourceLocation projId = ForgeRegistries.ENTITY_TYPES.getKey(directEntity.getType());
            if (projId != null && (projId.getNamespace().equals("cgs") || projId.getNamespace().equals("ntgl")
                    || projId.getNamespace().equals("createbigcannons") || projId.getPath().contains("bullet")
                    || projId.getPath().contains("rocket") || projId.getPath().contains("projectile"))) {
                return MobArchetypes.AttackCategory.CGS_FIREARM;
            }
        }

        if (directEntity instanceof AbstractArrow || damageTypeId.contains("arrow")) {
            return MobArchetypes.AttackCategory.MELEE_STRIKE;
        }
        if (damageTypeId.equals("minecraft:player_attack") || damageTypeId.equals("minecraft:mob_attack")
                || (msgId.equals("player") && !damageTypeId.contains("spell") && !damageTypeId.contains("magic"))) {
            return MobArchetypes.AttackCategory.MELEE_STRIKE;
        }

        return MobArchetypes.AttackCategory.GENERIC;
    }

    private static boolean isProfileMatch(MobArchetypes.Type archetype, MobArchetypes.AttackCategory category) {
        return switch (archetype) {
            case ARMORED -> category == MobArchetypes.AttackCategory.CGS_FIREARM;
            case ETHEREAL -> category == MobArchetypes.AttackCategory.MAGIC_SPELL;
            case BIO -> category == MobArchetypes.AttackCategory.MELEE_STRIKE;
            default -> false;
        };
    }

    private static void triggerShieldHitEffects(ServerLevel level, LivingEntity target, MobArchetypes.Type archetype, boolean isProfileHit) {
        double x = target.getX();
        double y = target.getY() + (target.getBbHeight() * 0.5D);
        double z = target.getZ();

        switch (archetype) {
            case ARMORED -> {
                level.playSound(null, x, y, z, SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 0.6F, isProfileHit ? 1.5F : 0.8F);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, isProfileHit ? 8 : 3, 0.2D, 0.2D, 0.2D, 0.05D);
            }
            case ETHEREAL -> {
                level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 0.7F, isProfileHit ? 1.4F : 0.7F);
                level.sendParticles(ParticleTypes.WITCH, x, y, z, isProfileHit ? 10 : 3, 0.2D, 0.2D, 0.2D, 0.02D);
            }
            case BIO -> {
                level.playSound(null, x, y, z, SoundEvents.SLIME_BLOCK_HIT, SoundSource.HOSTILE, 0.7F, isProfileHit ? 1.2F : 0.6F);
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, isProfileHit ? 12 : 4, 0.3D, 0.3D, 0.3D, 0.01D);
            }
            default -> {}
        }
    }

    private static void triggerShieldBreakEffects(ServerLevel level, LivingEntity target, MobArchetypes.Type archetype) {
        double x = target.getX();
        double y = target.getY() + (target.getBbHeight() * 0.5D);
        double z = target.getZ();

        switch (archetype) {
            case ARMORED -> {
                level.playSound(null, x, y, z, SoundEvents.ANVIL_DESTROY, SoundSource.HOSTILE, 1.0F, 1.1F);
                level.sendParticles(ParticleTypes.CRIT, x, y, z, 20, 0.4D, 0.4D, 0.4D, 0.15D);
                level.sendParticles(ParticleTypes.SMOKE, x, y, z, 15, 0.3D, 0.4D, 0.3D, 0.05D);
            }
            case ETHEREAL -> {
                level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.2F, 0.9F);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 25, 0.4D, 0.5D, 0.4D, 0.1D);
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, x, y, z, 15, 0.3D, 0.3D, 0.3D, 0.1D);
            }
            case BIO -> {
                level.playSound(null, x, y, z, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 0.9F, 1.3F);
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 15, 0.4D, 0.4D, 0.4D, 0.05D);
                target.removeEffect(MobEffects.REGENERATION);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 2, false, false));
            }
            default -> {}
        }
    }
}