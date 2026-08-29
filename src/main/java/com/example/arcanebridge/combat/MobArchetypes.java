package com.example.arcanebridge.combat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public class MobArchetypes {
    public static final String MODID = "arcane_bridge";

    // Строковые теги для ручного добавления через /tag @e[...] add <tag> или /summon ... {Tags:[...]}
    public static final String TAG_ARMORED = "arcane_armored";
    public static final String TAG_ETHEREAL = "arcane_ethereal";
    public static final String TAG_BIO = "arcane_bio";

    // Опциональные JSON-теги типов сущностей (fallback)
    public static final TagKey<EntityType<?>> ARMORED_TAG = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("arcane", "armored_automaton"));
    public static final TagKey<EntityType<?>> ETHEREAL_TAG = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("arcane", "ethereal_entity"));
    public static final TagKey<EntityType<?>> BIO_TAG = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("arcane", "bio_mutant"));

    // NBT-ключи в persistentData моба
    public static final String NBT_ARCHETYPE = "ArcaneArchetype";
    public static final String NBT_SHIELD_HP = "ArcaneShieldHP";
    public static final String NBT_MAX_SHIELD_HP = "ArcaneMaxShieldHP";
    public static final String NBT_SHIELD_BROKEN = "ArcaneShieldBroken";

    // Базовые значения емкости барьеров (Shield HP)
    public static final float HP_ARMORED_SHIELD = 12.0f; // ~1-2 выстрела Create Gunsmithing
    public static final float HP_ETHEREAL_SHIELD = 15.0f; // ~1 боевой каст Hex / Ars
    public static final float HP_BIO_SHIELD = 10.0f;      // ~2 акцентированных удара в ближнем бою

    // Множитель сопротивления при активном щите для непрофильного оружия (20% входящего урона)
    public static final float SHIELD_DAMAGE_REDUCTION = 0.20f;

    public enum Type {
        NONE,
        ARMORED,
        ETHEREAL,
        BIO
    }

    public enum AttackCategory {
        CGS_FIREARM,   // Огнестрел Create: Gunsmithing
        MAGIC_SPELL,   // Магия Hex Casting / Ars Nouveau
        MELEE_STRIKE,  // Ближний бой / Кулаки Cyberware / Мечи
        GENERIC        // Окружение, огонь, кактусы и прочее
    }

    /**
     * Быстрое разрешение архетипа сущности с приоритетом ручных тегов
     */
    public static Type resolveArchetype(LivingEntity entity) {
        if (entity.getTags().contains(TAG_ARMORED)) return Type.ARMORED;
        if (entity.getTags().contains(TAG_ETHEREAL)) return Type.ETHEREAL;
        if (entity.getTags().contains(TAG_BIO)) return Type.BIO;

        if (entity.getType().is(ARMORED_TAG)) return Type.ARMORED;
        if (entity.getType().is(ETHEREAL_TAG)) return Type.ETHEREAL;
        if (entity.getType().is(BIO_TAG)) return Type.BIO;

        return Type.NONE;
    }
}