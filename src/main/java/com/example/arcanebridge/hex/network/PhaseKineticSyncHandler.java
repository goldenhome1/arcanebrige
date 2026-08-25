package com.example.arcanebridge.hex.network;

import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class PhaseKineticSyncHandler {
    // Автономная синхронизация кинетики теперь полностью управляется через PhaseNetworkManager и PhaseRelayBlockEntity.tick()
}