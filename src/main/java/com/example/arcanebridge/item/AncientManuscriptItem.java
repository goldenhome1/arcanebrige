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

    public record ManuscriptLore(String visualEffect, String subtitle) {}

    private static final java.util.Map<String, ManuscriptLore> LORE_REGISTRY = java.util.Map.of(
            "phase_kinetics", new ManuscriptLore(
                    "§eВнутри пульсирует золотой орнамент двенадцатилучевой звезды.",
                    "§8«Сервисный оттиск фазового ретранслятора Архитекторов»"
            ),
            "phase_fluidics", new ManuscriptLore(
                    "§bОт плиты веет леденящим холодом с мерцающими лазурными рунами.",
                    "§8«Сервисная матрица крио-стабилизатора Архитекторов»"
            )
    );

    private final String descriptionKey;

    public AncientManuscriptItem(Properties properties, String descriptionKey) {
        super(properties.stacksTo(16).rarity(Rarity.UNCOMMON));
        this.descriptionKey = descriptionKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag isAdvanced) {
        tooltip.add(Component.literal("§7Древняя шестиугольная сланцевая плита."));

        ManuscriptLore lore = LORE_REGISTRY.get(this.descriptionKey);
        if (lore != null) {
            tooltip.add(Component.literal(lore.visualEffect()));
            tooltip.add(Component.literal(lore.subtitle()));
        } else {
            tooltip.add(Component.literal("§7На поверхности слабо мерцают неразборчивые символы."));
        }

        tooltip.add(Component.literal("§6• Возьмите в руку и покажите Мастеру Резонанса.").withStyle(ChatFormatting.ITALIC));
        super.appendHoverText(stack, level, tooltip, isAdvanced);
    }
}