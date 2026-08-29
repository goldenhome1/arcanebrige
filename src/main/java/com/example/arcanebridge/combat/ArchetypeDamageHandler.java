package com.example.arcanebridge.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = MobArchetypes.MODID)
public class ArchetypeDamageHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || target instanceof Player) return;

        CompoundTag data = target.getPersistentData();
        MobArchetypes.Type archetype = MobArchetypes.Type.NONE;

        // 1. Чтение или динамическая инициализация архетипа
        if (data.contains(MobArchetypes.NBT_ARCHETYPE)) {
            try {
                archetype = MobArchetypes.Type.valueOf(data.getString(MobArchetypes.NBT_ARCHETYPE));
            } catch (IllegalArgumentException ignored) {}
        } else {
            archetype = MobArchetypes.resolveArchetype(target);
            if (archetype != MobArchetypes.Type.NONE) {
                float maxHp = switch (archetype) {
                    case ARMORED -> MobArchetypes.HP_ARMORED_SHIELD;
                    case ETHEREAL -> MobArchetypes.HP_ETHEREAL_SHIELD;
                    case BIO -> MobArchetypes.HP_BIO_SHIELD;
                    default -> 0.0f;
                };
                data.putString(MobArchetypes.NBT_ARCHETYPE, archetype.name());
                data.putFloat(MobArchetypes.NBT_SHIELD_HP, maxHp);
                data.putFloat(MobArchetypes.NBT_MAX_SHIELD_HP, maxHp);
                data.putBoolean(MobArchetypes.NBT_SHIELD_BROKEN, false);
            }
        }

        // Если у моба нет архетипа или щит уже окончательно расколот — урон проходит штатно (100%)
        if (archetype == MobArchetypes.Type.NONE || data.getBoolean(MobArchetypes.NBT_SHIELD_BROKEN)) {
            return;
        }

        DamageSource source = event.getSource();
        MobArchetypes.AttackCategory category = classifyAttack(source);
        boolean isMatchingKey = isProfileMatch(archetype, category);

        float incomingDamage = event.getAmount();
        float currentShieldHp = data.getFloat(MobArchetypes.NBT_SHIELD_HP);
        ServerLevel level = (ServerLevel) target.level();

        if (isMatchingKey) {
            // =========================================================================
            // ПРОФИЛЬНАЯ АТАКА (100% УРОНА ПО ЩИТУ)
            // =========================================================================
            currentShieldHp -= incomingDamage;
            data.putFloat(MobArchetypes.NBT_SHIELD_HP, Math.max(0.0f, currentShieldHp));

            if (currentShieldHp <= 0.0f) {
                // Раскол барьера
                data.putBoolean(MobArchetypes.NBT_SHIELD_BROKEN, true);
                triggerShieldBreakEffects(level, target, archetype);
            } else {
                // Промежуточное попадание профильным оружием (треск/вспышка)
                triggerShieldHitEffects(level, target, archetype, true);
            }
        } else {
            // =========================================================================
            // НЕПРОФИЛЬНАЯ АТАКА (20% УРОНА ПО ЩИТУ И ЗДОРОВЬЮ)
            // =========================================================================
            float reducedDamage = incomingDamage * MobArchetypes.SHIELD_DAMAGE_REDUCTION;
            event.setAmount(reducedDamage);

            currentShieldHp -= reducedDamage;
            data.putFloat(MobArchetypes.NBT_SHIELD_HP, Math.max(0.0f, currentShieldHp));

            if (currentShieldHp <= 0.0f) {
                data.putBoolean(MobArchetypes.NBT_SHIELD_BROKEN, true);
                triggerShieldBreakEffects(level, target, archetype);
            } else {
                // Глухой отклик поглощения
                triggerShieldHitEffects(level, target, archetype, false);
            }
        }
    }

    /**
     * Определение источника урона
     */
    private static MobArchetypes.AttackCategory classifyAttack(DamageSource source) {
        Entity directEntity = source.getDirectEntity();
        Entity trueSource = source.getEntity();

        // 1. Огнестрел Create: Gunsmithing (снаряды или идентификаторы CGS)
        if (directEntity instanceof Projectile) {
            ResourceLocation projId = ForgeRegistries.ENTITY_TYPES.getKey(directEntity.getType());
            if (projId != null && (projId.getNamespace().equals("cgs") || projId.getPath().contains("bullet") || projId.getPath().contains("round"))) {
                return MobArchetypes.AttackCategory.CGS_FIREARM;
            }
        }
        String damageMsg = source.getMsgId();
        if (damageMsg.contains("cgs") || damageMsg.contains("bullet") || damageMsg.contains("gun")) {
            return MobArchetypes.AttackCategory.CGS_FIREARM;
        }

        // 2. Магия (Hex Casting / Ars Nouveau / теги DamageTypeTags.IS_MAGIC)
        if (source.is(DamageTypeTags.IS_MAGIC) || damageMsg.contains("hexcasting") || damageMsg.contains("ars_nouveau") || damageMsg.contains("magic")) {
            return MobArchetypes.AttackCategory.MAGIC_SPELL;
        }
        if (directEntity != null) {
            ResourceLocation projId = ForgeRegistries.ENTITY_TYPES.getKey(directEntity.getType());
            if (projId != null && (projId.getNamespace().equals("ars_nouveau") || projId.getNamespace().equals("hexcasting"))) {
                return MobArchetypes.AttackCategory.MAGIC_SPELL;
            }
        }

        // 3. Ближний бой (контактный удар от игрока или сущности, не являющийся снарядом)
        if (directEntity != null && directEntity == trueSource && trueSource instanceof LivingEntity) {
            return MobArchetypes.AttackCategory.MELEE_STRIKE;
        }

        return MobArchetypes.AttackCategory.GENERIC;
    }

    /**
     * Проверка соответствия ключа уязвимости
     */
    private static boolean isProfileMatch(MobArchetypes.Type archetype, MobArchetypes.AttackCategory category) {
        return switch (archetype) {
            case ARMORED -> category == MobArchetypes.AttackCategory.CGS_FIREARM;
            case ETHEREAL -> category == MobArchetypes.AttackCategory.MAGIC_SPELL;
            case BIO -> category == MobArchetypes.AttackCategory.MELEE_STRIKE;
            default -> false;
        };
    }

    /**
     * Звуки и частицы при обычном попадании
     */
    private static void triggerShieldHitEffects(ServerLevel level, LivingEntity target, MobArchetypes.Type archetype, boolean isProfileHit) {
        double x = target.getX();
        double y = target.getY() + (target.getBbHeight() * 0.5);
        double z = target.getZ();

        switch (archetype) {
            case ARMORED -> {
                level.playSound(null, x, y, z, SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 0.6f, isProfileHit ? 1.5f : 0.8f);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, isProfileHit ? 8 : 3, 0.2, 0.2, 0.2, 0.05);
            }
            case ETHEREAL -> {
                level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 0.7f, isProfileHit ? 1.4f : 0.7f);
                level.sendParticles(ParticleTypes.WITCH, x, y, z, isProfileHit ? 10 : 3, 0.2, 0.2, 0.2, 0.02);
            }
            case BIO -> {
                level.playSound(null, x, y, z, SoundEvents.SLIME_BLOCK_HIT, SoundSource.HOSTILE, 0.7f, isProfileHit ? 1.2f : 0.6f);
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, isProfileHit ? 12 : 4, 0.3, 0.3, 0.3, 0.01);
            }
            default -> {}
        }
    }

    /**
     * Звуки, частицы и статусы при полном расколе барьера (Shield HP <= 0)
     */
    private static void triggerShieldBreakEffects(ServerLevel level, LivingEntity target, MobArchetypes.Type archetype) {
        double x = target.getX();
        double y = target.getY() + (target.getBbHeight() * 0.5);
        double z = target.getZ();

        switch (archetype) {
            case ARMORED -> {
                level.playSound(null, x, y, z, SoundEvents.ANVIL_DESTROY, SoundSource.HOSTILE, 1.0f, 1.1f);
                level.sendParticles(ParticleTypes.CRIT, x, y, z, 20, 0.4, 0.4, 0.4, 0.15);
                level.sendParticles(ParticleTypes.SMOKE, x, y, z, 15, 0.3, 0.4, 0.3, 0.05);
            }
            case ETHEREAL -> {
                level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.2f, 0.9f);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 25, 0.4, 0.5, 0.4, 0.1);
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, x, y, z, 15, 0.3, 0.3, 0.3, 0.1);
            }
            case BIO -> {
                level.playSound(null, x, y, z, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 0.9f, 1.3f);
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 15, 0.4, 0.4, 0.4, 0.05);
                // Отключаем регенерацию и накладываем микро-стан (замедление IV на 1 секунду)
                target.removeEffect(MobEffects.REGENERATION);
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3, false, false));
            }
            default -> {}
        }
    }
}