package com.example.arcanebridge.registry;

import at.petrak.hexcasting.api.casting.ActionRegistryEntry;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.common.lib.HexRegistries;
import com.example.arcanebridge.ArcaneBridge;
import com.example.arcanebridge.hex.actions.OpPhaseFluidRX;
import com.example.arcanebridge.hex.actions.OpPhaseFluidTX;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModHexActions {
    public static final DeferredRegister<ActionRegistryEntry> ACTIONS =
            DeferredRegister.create(HexRegistries.ACTION, ArcaneBridge.MODID);

    // Паттерн Источника (TX) из Hex (2).hexcasting
    public static final RegistryObject<ActionRegistryEntry> PHASE_FLUID_TX = ACTIONS.register("phase_fluid_tx",
            () -> new ActionRegistryEntry(
                    HexPattern.fromAngles("wwawwqwwawwwwwawwwwwawwewwawwwwwaww", HexDir.EAST),
                    OpPhaseFluidTX.INSTANCE
            ));

    // Паттерн Приёмника (RX) из Hex (3).hexcasting
    public static final RegistryObject<ActionRegistryEntry> PHASE_FLUID_RX = ACTIONS.register("phase_fluid_rx",
            () -> new ActionRegistryEntry(
                    HexPattern.fromAngles("wwawwqwwawwwwwawwwwwawwewwawwwwwawwqeaqaaw", HexDir.EAST),
                    OpPhaseFluidRX.INSTANCE
            ));

    public static void register(IEventBus eventBus) {
        ACTIONS.register(eventBus);
    }
}