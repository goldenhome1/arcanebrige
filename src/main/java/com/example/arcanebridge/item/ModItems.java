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

        public static final RegistryObject<Item> DECIPHERED_SCROLL_PHASE = ITEMS.register("deciphered_scroll_phase",
            () -> new DecipheredManuscriptItem(
                    new Item.Properties(),
                    new net.minecraft.resources.ResourceLocation("arcane_bridge", "spells/phase_kinetics"),
                    "Фазовый Резонанс (Кинетика)"
            ));

    public static final RegistryObject<Item> ANCIENT_SCROLL_FLUID = ITEMS.register("ancient_scroll_fluid",
            () -> new AncientManuscriptItem(new Item.Properties(), "phase_fluidics"));

        public static final RegistryObject<Item> DECIPHERED_SCROLL_FLUID = ITEMS.register("deciphered_scroll_fluid",
            () -> new DecipheredManuscriptItem(
                    new Item.Properties(),
                    new net.minecraft.resources.ResourceLocation("arcane_bridge", "spells/phase_fluidics"),
                    "Фазовый Резонанс (Гидродинамика)"
            ));

        public static final RegistryObject<Item> PHASE_FLUID_RELAY = ITEMS.register("phase_fluid_relay",
            () -> new net.minecraft.world.item.BlockItem(
                    com.example.arcanebridge.registry.ModBlocks.PHASE_FLUID_RELAY.get(),
                    new Item.Properties()
            ));

    public static final RegistryObject<Item> PHASE_FLUID_TUNER = ITEMS.register("phase_fluid_tuner",
            () -> new PhaseFluidTunerItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}