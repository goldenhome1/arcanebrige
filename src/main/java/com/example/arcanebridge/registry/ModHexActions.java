package com.example.arcanebridge.registry;

import at.petrak.hexcasting.api.casting.ActionRegistryEntry;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.common.lib.HexRegistries;
import com.example.arcanebridge.ArcaneBridge;
import com.example.arcanebridge.hex.actions.OpPhaseFluidClose;
import com.example.arcanebridge.hex.actions.OpPhaseFluidOpen;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModHexActions {
    public static final DeferredRegister<ActionRegistryEntry> ACTIONS =
            DeferredRegister.create(HexRegistries.ACTION, ArcaneBridge.MODID);

    // Паттерн Открытия/Связывания Гидравлики
    public static final RegistryObject<ActionRegistryEntry> PHASE_FLUID_OPEN = ACTIONS.register("phase_fluid_open",
            () -> new ActionRegistryEntry(
                    HexPattern.fromAngles("wwawwqwwawwwwwawwwwwawwewwawwwwwaww", HexDir.NORTH_EAST),
                    OpPhaseFluidOpen.INSTANCE
            ));

    // Паттерн Закрытия/Разрыва Гидравлики
    public static final RegistryObject<ActionRegistryEntry> PHASE_FLUID_CLOSE = ACTIONS.register("phase_fluid_close",
            () -> new ActionRegistryEntry(
                    HexPattern.fromAngles("wwawwqwwawwwwwawwwwwawwewwawwwwwawwqeaqaaw", HexDir.NORTH_EAST),
                    OpPhaseFluidClose.INSTANCE
            ));

    public static void register(IEventBus eventBus) {
        ACTIONS.register(eventBus);
    }
}