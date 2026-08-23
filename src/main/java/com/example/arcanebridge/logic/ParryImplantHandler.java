package com.example.arcanebridge.logic;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class ParryImplantHandler {

    private static final String REFLEX_IMPLANT = "cyber_ware_port:muscle_upgrades_wired_reflexes";
    private static final ResourceLocation PARRY_ATTR_ID = new ResourceLocation("just_parry", "enableparry");

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }

        // Проверяем каждые 5 тиков (4 раза в секунду)
        if (event.player.tickCount % 5 != 0) {
            return;
        }

        if (event.player instanceof ServerPlayer player) {
            Attribute parryAttribute = ForgeRegistries.ATTRIBUTES.getValue(PARRY_ATTR_ID);
            if (parryAttribute == null) return;

            AttributeInstance instance = player.getAttribute(parryAttribute);
            if (instance == null) return;

            boolean hasImplant = CyberwareHelper.hasCyberware(player, REFLEX_IMPLANT);
            double targetBase = hasImplant ? 1.0D : 0.0D;

            // Синхронизируем базовое значение (Vanilla автоматически уведомит клиент)
            if (Double.compare(instance.getBaseValue(), targetBase) != 0) {
                instance.setBaseValue(targetBase);
            }
        }
    }
}