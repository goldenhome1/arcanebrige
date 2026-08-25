package com.example.arcanebridge.hex;

import at.petrak.hexcasting.api.casting.ActionRegistryEntry;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.common.lib.HexRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModHexActions {
    public static final DeferredRegister<ActionRegistryEntry> ACTIONS =
            DeferredRegister.create(HexRegistries.ACTION, "arcane_bridge");

    // Паттерн Открытия / TX: "adaeadaeadaeadaeadaeadawqwqwqwqwqw"
    public static final RegistryObject<ActionRegistryEntry> TUNE_TRANSMITTER = ACTIONS.register("tune_transmitter",
            () -> new ActionRegistryEntry(
                    HexPattern.fromAngles("adaeadaeadaeadaeadaeadawqwqwqwqwqw", HexDir.NORTH_EAST),
                    new OpTuneTransmitter()
            ));

    // Паттерн Закрытия / RX: "adaeadaeadaeadaeadaeadawqwqwqwqwaeqqqqq"
    public static final RegistryObject<ActionRegistryEntry> TUNE_RECEIVER = ACTIONS.register("tune_receiver",
            () -> new ActionRegistryEntry(
                    HexPattern.fromAngles("adaeadaeadaeadaeadaeadawqwqwqwqwaeqqqqq", HexDir.NORTH_EAST),
                    new OpTuneReceiver()
            ));

    public static void register(IEventBus bus) {
        ACTIONS.register(bus);
    }
}