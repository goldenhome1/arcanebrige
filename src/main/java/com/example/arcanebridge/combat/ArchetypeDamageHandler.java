            // ПРОФИЛЬНАЯ АТАКА (100% УРОНА ПО ЩИТУ)
            // =========================================================================
            currentShieldHp -= incomingDamage;
            data.putFloat(MobArchetypes.NBT_SHIELD_HP, Math.max(0.0f, currentShieldHp));

            if (currentShieldHp <= 0.0f) {
                data.putBoolean(MobArchetypes.NBT_SHIELD_BROKEN, true);
                triggerShieldBreakEffects(level, target, archetype);
            } else {
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
                triggerShieldHitEffects(level, target, archetype, false);
            }
        }
    }
=======
package com.example.arcanebridge.combat;

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

        // 1. Проверка полного отсутствия щитов
        if (data.getBoolean(MobArchetypes.NBT_ALL_SHIELDS_BROKEN)) {
            return;
        }

        // 2. Инициализация стека щитов, если он еще не создан
        if (!data.contains(MobArchetypes.NBT_SHIELD_LAYERS, Tag.TAG_LIST)) {
            initShieldStack(target, data);
        }

        ListTag layers = data.getList(MobArchetypes.NBT_SHIELD_LAYERS, Tag.TAG_COMPOUND);
        int currentIndex = data.getInt(MobArchetypes.NBT_CURRENT_LAYER_INDEX);

        if (layers.isEmpty() || currentIndex >= layers.size()) {
            data.putBoolean(MobArchetypes.NBT_ALL_SHIELDS_BROKEN, true);
            return;
        }

        // 3. Получение текущего активного внешнего слоя
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

        float incomingDamage = event.getAmount();
        float currentShieldHp = activeLayer.getFloat("HP");
        ServerLevel level = (ServerLevel) target.level();

        if (isMatchingKey) {
            // =========================================================================
            // ПРОФИЛЬНАЯ АТАКА (100% УРОНА ПО ТЕКУЩЕМУ ЩИТУ)
            // =========================================================================
            currentShieldHp -= incomingDamage;
            activeLayer.putFloat("HP", Math.max(0.0f, currentShieldHp));

            if (currentShieldHp <= 0.0f) {
                // Текущий слой разрушен
                triggerShieldBreakEffects(level, target, archetype);
                currentIndex++;
                data.putInt(MobArchetypes.NBT_CURRENT_LAYER_INDEX, currentIndex);

                if (currentIndex >= layers.size()) {
                    data.putBoolean(MobArchetypes.NBT_ALL_SHIELDS_BROKEN, true);
                    data.putBoolean(MobArchetypes.NBT_SHIELD_BROKEN, true);
                } else {
                    // Звук обнажения следующего внутреннего барьера
                    level.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 0.8f, 1.4f);
                }
            } else {
                triggerShieldHitEffects(level, target, archetype, true);
            }
        } else {
            // =========================================================================
            // НЕПРОФИЛЬНАЯ АТАКА (20% УРОНА ПО ЩИТУ И ЗДОРОВЬЮ)
            // =========================================================================
            float reducedDamage = incomingDamage * MobArchetypes.SHIELD_DAMAGE_REDUCTION;
            event.setAmount(reducedDamage);

            currentShieldHp -= reducedDamage;
            activeLayer.putFloat("HP", Math.max(0.0f, currentShieldHp));

            if (currentShieldHp <= 0.0f) {
                triggerShieldBreakEffects(level, target, archetype);
                currentIndex++;
                data.putInt(MobArchetypes.NBT_CURRENT_LAYER_INDEX, currentIndex);

                if (currentIndex >= layers.size()) {
                    data.putBoolean(MobArchetypes.NBT_ALL_SHIELDS_BROKEN, true);
                    data.putBoolean(MobArchetypes.NBT_SHIELD_BROKEN, true);
                } else {
                    level.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 0.8f, 1.4f);
                }
            } else {
                triggerShieldHitEffects(level, target, archetype, false);
            }
        }
    }

    /**
     * Построение стека слоев из тегов моба или базового архетипа
     */
    private static void initShieldStack(LivingEntity entity, CompoundTag data) {
        ListTag layers = new ListTag();

        // 1. Проверяем наличие составных тегов (Комбинированный барьер)
        // Пример порядка слоев: снаружи Эфир -> внутри Броня
        if (entity.getTags().contains(MobArchetypes.TAG_ETHEREAL)) {
            float hp = data.contains("ArcaneEtherealHP") ? data.getFloat("ArcaneEtherealHP") : MobArchetypes.HP_ETHEREAL_SHIELD;
            layers.add(createLayerTag(MobArchetypes.Type.ETHEREAL, hp));
        }
        if (entity.getTags().contains(MobArchetypes.TAG_ARMORED)) {
            float hp = data.contains("ArcaneArmoredHP") ? data.getFloat("ArcaneArmoredHP") : MobArchetypes.HP_ARMORED_SHIELD;
            layers.add(createLayerTag(MobArchetypes.Type.ARMORED, hp));
        }
        if (entity.getTags().contains(MobArchetypes.TAG_BIO)) {
            float hp = data.contains("ArcaneBioHP") ? data.getFloat("ArcaneBioHP") : MobArchetypes.HP_BIO_SHIELD;
            layers.add(createLayerTag(MobArchetypes.Type.BIO, hp));
        }

        // 2. Если ручных тегов нет, проверяем EntityType Tags из датапаков
        if (layers.isEmpty()) {
            MobArchetypes.Type defaultType = MobArchetypes.resolveArchetype(entity);
            if (defaultType != MobArchetypes.Type.NONE) {
                float defaultHp = switch (defaultType) {
                    case ARMORED -> MobArchetypes.HP_ARMORED_SHIELD;
                    case ETHEREAL -> MobArchetypes.HP_ETHEREAL_SHIELD;
                    case BIO -> MobArchetypes.HP_BIO_SHIELD;
                    default -> 0.0f;
                };
                layers.add(createLayerTag(defaultType, defaultHp));
            }
        }

        data.put(MobArchetypes.NBT_SHIELD_LAYERS, layers);
        data.putInt(MobArchetypes.NBT_CURRENT_LAYER_INDEX, 0);
        data.putBoolean(MobArchetypes.NBT_ALL_SHIELDS_BROKEN, layers.isEmpty());
    }

    private static CompoundTag createLayerTag(MobArchetypes.Type type, float hp) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", type.name());
        tag.putFloat("HP", hp);
        tag.putFloat("MaxHP", hp);
        return tag;
    }=======
            // ПРОФИЛЬНАЯ АТАКА (100% УРОНА ПО ЩИТУ)
            // =========================================================================
            currentShieldHp -= incomingDamage;
            data.putFloat(MobArchetypes.NBT_SHIELD_HP, Math.max(0.0f, currentShieldHp));

            if (currentShieldHp <= 0.0f) {
                data.putBoolean(MobArchetypes.NBT_SHIELD_BROKEN, true);
                triggerShieldBreakEffects(level, target, archetype);
            } else {
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
                triggerShieldHitEffects(level, target, archetype, false);
            }
        }
    }

    /**
     * Пошаговая классификация входящей атаки
     */
    private static MobArchetypes.AttackCategory classifyAttack(LivingEntity target, DamageSource source) {
        String damageTypeId = "";
        try {
            ResourceLocation key = target.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getKey(source.type());
            if (key != null) {
                damageTypeId = key.toString();
            }
        } catch (Exception ignored) {}

        String msgId = source.getMsgId();
        Entity directEntity = source.getDirectEntity();

        // 1. ШАГ 1: МАГИЯ (Ars Nouveau, Hex Casting, ванильная магия)
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

        // 2. ШАГ 2: ОГНЕСТРЕЛ И АРТИЛЛЕРИЯ (Create: Gunsmithing, NTGL, CBC)
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

        // 3. ШАГ 3: БЛИЖНИЙ БОЙ И СТРЕЛЫ / БОЛТЫ (Уязвимость Био-мутанта)
        // Ванильные стрелы и болты
        if (directEntity instanceof AbstractArrow || damageTypeId.contains("arrow")) {
            return MobArchetypes.AttackCategory.MELEE_STRIKE;
        }
        // Прямой контактный удар мечом, топором, кулаком или лапой моба
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
                target.removeEffect(MobEffects.REGENERATION);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3, false, false));
            }
            default -> {}
        }
    }
}