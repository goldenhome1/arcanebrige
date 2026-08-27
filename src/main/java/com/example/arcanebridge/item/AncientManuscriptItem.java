package com.example.arcanebridge.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AncientManuscriptItem extends Item {

    private final String descriptionKey;

    public AncientManuscriptItem(Properties properties, String descriptionKey) {
        super(properties.stacksTo(16).rarity(Rarity.UNCOMMON));
        this.descriptionKey = descriptionKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag isAdvanced) {
        tooltip.add(Component.literal("§7Древний фрагмент рунических записей."));
        tooltip.add(Component.literal("§dСимволы зашифрованы неизвестным диалектом эфира."));
        tooltip.add(Component.literal("§6• Отдайте Мастеру Резонанса для перевода.").withStyle(ChatFormatting.ITALIC));
        super.appendHoverText(stack, level, tooltip, isAdvanced);
    }
}