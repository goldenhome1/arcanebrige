package com.example.arcanebridge.item;

import com.example.arcanebridge.ArcaneBridge;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ArcaneBridge.MODID);

        public static final RegistryObject<Item> GUIDE_CORE = ITEMS.register("guide_core",
            () -> new GuideCoreItem(new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> ANCIENT_SCROLL_PHASE = ITEMS.register("ancient_scroll_phase",
            () -> new AncientManuscriptItem(new Item.Properties(), "phase_kinetics"));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}