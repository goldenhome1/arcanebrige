package com.example.arcanebridge.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "arcane_bridge");

    public static final RegistryObject<EntityType<ArcaneGuideEntity>> ARCANE_GUIDE =
            ENTITIES.register("arcane_guide", () -> EntityType.Builder.of(ArcaneGuideEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build("arcane_guide"));

    public static final RegistryObject<EntityType<ConsortiumMerchantEntity>> CONSORTIUM_MERCHANT =
            ENTITIES.register("consortium_merchant", () -> EntityType.Builder.of(ConsortiumMerchantEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build("consortium_merchant"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}