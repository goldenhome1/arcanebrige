package com.example.arcanebridge.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = "arcane_bridge", value = Dist.CLIENT)
public class ArmorJamTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !stack.hasTag()) return;

        if (stack.getTag() != null && stack.getTag().getBoolean("Jammed")) {
            List<Component> tooltip = event.getToolTip();
            int insertIndex = Math.min(1, tooltip.size());

            tooltip.add(insertIndex, Component.literal("§c§l⚠ [МЕХАНИЧЕСКИЙ СБОЙ]: ПРИВОДЫ ЗАКЛИНИЛИ!"));
            tooltip.add(insertIndex + 1, Component.literal(" §4• Все защитные показатели и модули отключены."));
            tooltip.add(insertIndex + 2, Component.literal(" §6• Ремонт: §7Возьмите броню в §eлевую руку§7, Гаечный Ключ Create в §eправую§7 и нажмите §fПКМ§7."));
            tooltip.add(insertIndex + 3, Component.literal(""));
        }
    }
}