package com.example.arcanebridge.registry;

import com.example.arcanebridge.block.entity.PhaseRelayBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "arcane_bridge");

    public static final RegistryObject<BlockEntityType<PhaseRelayBlockEntity>> PHASE_RELAY =
            BLOCK_ENTITIES.register("phase_relay",
                    () -> BlockEntityType.Builder.of(PhaseRelayBlockEntity::new, ModBlocks.PHASE_RELAY.get()).build(null));
}