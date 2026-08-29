package com.example.arcanebridge.registry;

import at.petrak.hexcasting.api.casting.ActionRegistryEntry;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.common.lib.HexRegistries;
import com.example.arcanebridge.ArcaneBridge;
import com.example.arcanebridge.hex.actions.OpCastPhaseReceiver;
import com.example.arcanebridge.hex.actions.OpCastPhaseTransmitter;
import com.example.arcanebridge.hex.actions.OpPhaseFluidRX;
import com.example.arcanebridge.hex.actions.OpPhaseFluidTX;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModHexActions {
    public static final DeferredRegister<ActionRegistryEntry> ACTIONS =
            DeferredRegister.create(HexRegistries.ACTION, ArcaneBridge.MODID);

    // ⚙️ КИНЕТИКА / ВАЛЫ (Оригинальные солнечные паттерны)
    public static final RegistryObject<ActionRegistryEntry> TUNE_TRANSMITTER = ACTIONS.register("tune_transmitter",
            () -> new ActionRegistryEntry(
                    HexPattern.fromAngles("adaeadaeadaeadaeadaeadawwqwqwqwqwqw", HexDir.NORTH_EAST),
                    new OpCastPhaseTransmitter()
            ));

    public static final RegistryObject<ActionRegistryEntry> TUNE_RECEIVER = ACTIONS.register("tune_receiver",
            () -> new ActionRegistryEntry(
                    HexPattern.fromAngles("adaeadaeadaeadaeadaeadawwqwqwqwqwqwaeqqqqq", HexDir.NORTH_EAST),
                    new OpCastPhaseReceiver()
            ));

    // 💧 ГИДРАВЛИКА / ТРУБЫ (Паттерны из Hex (2) и Hex (3))
    public static final RegistryObject<ActionRegistryEntry> PHASE_FLUID_TX = ACTIONS.register("phase_fluid_tx",
            () -> new ActionRegistryEntry(
                    HexPattern.fromAngles("wwawwqwwawwwwwawwwwwawwewwawwwwwaww", HexDir.EAST),
                    new OpPhaseFluidTX()
            ));

    public static final RegistryObject<ActionRegistryEntry> PHASE_FLUID_RX = ACTIONS.register("phase_fluid_rx",
            () -> new ActionRegistryEntry(
                    HexPattern.fromAngles("wwawwqwwawwwwwawwwwwawwewwawwwwwawwqeaqaaw", HexDir.EAST),
                    new OpPhaseFluidRX()
            ));

    public static void register(IEventBus eventBus) {
        ACTIONS.register(eventBus);
    }
}