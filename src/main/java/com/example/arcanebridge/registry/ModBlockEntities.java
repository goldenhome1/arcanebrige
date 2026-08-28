// Добавьте регистрацию BlockEntity:
public static final RegistryObject<BlockEntityType<com.example.arcanebridge.block.entity.PhaseFluidBlockEntity>> PHASE_FLUID_RELAY =
        BLOCK_ENTITIES.register("phase_fluid_relay",
                () -> BlockEntityType.Builder.of(
                        com.example.arcanebridge.block.entity.PhaseFluidBlockEntity::new,
                        ModBlocks.PHASE_FLUID_RELAY.get()
                ).build(null));