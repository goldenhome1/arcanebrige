package com.example.arcanebridge.hex;

import com.example.arcanebridge.hex.actions.OpCastPhaseReceiver;
import com.example.arcanebridge.hex.actions.OpCastPhaseTransmitter;
import com.example.arcanebridge.hex.actions.OpReadBlockTag;
import at.petrak.hexcasting.api.casting.ActionRegistryEntry;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.common.lib.HexRegistries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ArcaneHexRegistry {
    public static final DeferredRegister<ActionRegistryEntry> ACTIONS =
            DeferredRegister.create(HexRegistries.ACTION, "arcane_bridge");

        public static final RegistryObject<ActionRegistryEntry> READ_BLOCK_TAG =
            ACTIONS.register("read_block_tag", () -> new ActionRegistryEntry(
                    HexPattern.fromAngles("qawq", HexDir.NORTH_EAST),
                    new OpReadBlockTag()
            ));

                // 🌌 Великий Гекс Фазового Передатчика (TX / Открытие канала)
    public static final RegistryObject<ActionRegistryEntry> BIND_PHASE_TRANSMITTER =
            ACTIONS.register("bind_phase_transmitter", () -> new ActionRegistryEntry(
                    HexPattern.fromAngles("qqwdwqqqdqqqdqdqqwdwqqqdqqqqdaqweewq", HexDir.EAST),
                    new OpCastPhaseTransmitter()
            ));

        // 🔮 Великий Гекс Фазового Приемника (RX / Замыкание контура)
    public static final RegistryObject<ActionRegistryEntry> BIND_PHASE_RECEIVER =
            ACTIONS.register("bind_phase_receiver", () -> new ActionRegistryEntry(
                    HexPattern.fromAngles("qwdwqqqdqqqdqdqqwdwqqqdqqqqdqawqedqqdeqw", HexDir.EAST),
                    new OpCastPhaseReceiver()
            ));

    public static void register(IEventBus modEventBus) {
        ACTIONS.register(modEventBus);
    }
}
