package com.example.arcanebridge.registry;

import com.example.arcanebridge.block.PhaseRelayBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "arcane_bridge");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "arcane_bridge");

    public static final RegistryObject<Block> PHASE_RELAY = BLOCKS.register("phase_relay",
            () -> new PhaseRelayBlock(BlockBehaviour.Properties.of()
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.AMETHYST)
                    .noOcclusion()));

    public static final RegistryObject<Item> PHASE_RELAY_ITEM = ITEMS.register("phase_relay",
            () -> new BlockItem(PHASE_RELAY.get(), new Item.Properties()));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}