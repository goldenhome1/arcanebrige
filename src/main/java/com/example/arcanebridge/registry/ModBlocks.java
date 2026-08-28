package com.example.arcanebridge.registry;

import com.example.arcanebridge.ArcaneBridge;
import com.example.arcanebridge.block.PhaseFluidBlock;
import com.example.arcanebridge.block.PhaseRelayBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ArcaneBridge.MODID);

    public static final RegistryObject<Block> PHASE_RELAY = BLOCKS.register("phase_relay",
            () -> new PhaseRelayBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.0F)
                    .noOcclusion()));

    public static final RegistryObject<Block> PHASE_FLUID_RELAY = BLOCKS.register("phase_fluid_relay",
            () -> new PhaseFluidBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.0F)
                    .noOcclusion()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}