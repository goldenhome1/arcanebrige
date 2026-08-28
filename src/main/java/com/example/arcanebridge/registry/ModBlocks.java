// Добавьте регистрацию блока:
public static final RegistryObject<Block> PHASE_FLUID_RELAY = BLOCKS.register("phase_fluid_relay",
        () -> new com.example.arcanebridge.block.PhaseFluidBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(2.0F)
                .noOcclusion()));