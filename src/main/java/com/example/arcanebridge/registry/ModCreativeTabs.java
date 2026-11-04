package com.example.arcanebridge.registry;

import com.example.arcanebridge.ArcaneBridge;
import com.example.arcanebridge.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArcaneBridge.MODID);

    public static final RegistryObject<CreativeModeTab> ARCANE_BRIDGE_TAB = CREATIVE_MODE_TABS.register("arcane_bridge_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.NUMISMATIC_PATENT.get()))
                    .title(Component.translatable("itemGroup.arcane_bridge"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.NUMISMATIC_PATENT.get());
                        output.accept(ModItems.GUIDE_CORE.get());
                        output.accept(ModItems.ANCIENT_SCROLL_PHASE.get());
                        output.accept(ModItems.DECIPHERED_SCROLL_PHASE.get());
                        output.accept(ModItems.ANCIENT_SCROLL_FLUID.get());
                        output.accept(ModItems.DECIPHERED_SCROLL_FLUID.get());
                        output.accept(ModItems.PHASE_FLUID_RELAY.get());
                        output.accept(ModItems.PHASE_FLUID_TUNER.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES || event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.NUMISMATIC_PATENT.get());
        }
    }
}
