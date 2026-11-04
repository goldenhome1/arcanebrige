package com.example.arcanebridge.entity.merchant;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConsortiumTradeGenerator {

    public static Item getCoin(String name, Item fallback) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("numismatics", name));
        if (item == null || item == Items.AIR) {
            return fallback;
        }
        return item;
    }

    public static MerchantOffers generateDailyOffers(Entity merchant, RandomSource random) {
        MerchantOffers offers = new MerchantOffers();

        Item cog = getCoin("cog", Items.GOLD_INGOT);
        Item bevel = getCoin("bevel", Items.IRON_NUGGET);
        Item crown = getCoin("crown", Items.DIAMOND);

        // 1. Постоянный Слот 1: 1 Cog -> 1 Изумруд (Вечный якорь)
        offers.add(new MerchantOffer(
                new ItemStack(cog, 1),
                ItemStack.EMPTY,
                new ItemStack(Items.EMERALD, 1),
                999999, // Практически бесконечный запас
                0,
                0.0F
        ));

        // 2. Сбор доступных профессий жителей
        List<VillagerProfession> professions = new ArrayList<>();
        for (VillagerProfession prof : VillagerTrades.TRADES.keySet()) {
            if (prof != VillagerProfession.NONE && prof != VillagerProfession.NITWIT) {
                professions.add(prof);
            }
        }

        if (professions.isEmpty()) {
            return offers;
        }

        // 3. Выбор 5-6 уникальных предложений из пула профессий
        int targetTradeCount = 5 + random.nextInt(2); // 5 или 6 динамических лотов
        int attempts = 0;
        int maxAttempts = 150;

        while (offers.size() <= targetTradeCount && attempts < maxAttempts) {
            attempts++;

            VillagerProfession prof = professions.get(random.nextInt(professions.size()));
            Int2ObjectMap<VillagerTrades.ItemListing[]> levelMap = VillagerTrades.TRADES.get(prof);
            if (levelMap == null || levelMap.isEmpty()) continue;

            // Выбираем уровень (1-5, с уклоном в более интересные уровни 2-5)
            int level = 1 + random.nextInt(5);
            VillagerTrades.ItemListing[] listings = levelMap.get(level);
            if (listings == null || listings.length == 0) continue;

            VillagerTrades.ItemListing listing = listings[random.nextInt(listings.length)];
            MerchantOffer rawOffer = listing.getOffer(merchant, random);
            if (rawOffer == null) continue;

            // Нам нужны ТОЛЬКО товары, продаваемые игроку за изумруды (не скупка!)
            if (rawOffer.getResult().is(Items.EMERALD)) {
                continue; // Это скупка за изумруды, пропускаем
            }

            if (!rawOffer.getBaseCostA().is(Items.EMERALD)) {
                continue; // Если цена не в изумрудах, пропускаем
            }

            int emeralds = rawOffer.getBaseCostA().getCount();
            ItemStack coinCost;

            // --- Нелинейная шкала престижа ---
            if (emeralds <= 3) {
                // Дешевая мелочевка (стрелы, хлеб, красители): 1-3 Bevels
                coinCost = new ItemStack(bevel, Math.max(1, emeralds));
            } else if (emeralds <= 15) {
                // Средний класс (инструменты, броня, карты): 4-15 Cogs
                coinCost = new ItemStack(cog, emeralds);
            } else {
                // Джекпот и Элита (Алмазка, Починка, редкие чары): 2-5 Crowns
                // При 16-24 изумрудах -> 2-3 Crown, при 25-35 -> 4 Crown, при 36+ -> 5-6 Crown
                int crownCount = Math.max(2, Math.min(6, (int) Math.round(emeralds / 7.0)));
                coinCost = new ItemStack(crown, crownCount);
            }

            // Копируем дополнительный расходник (например, книгу для зачарованных свитков)
            ItemStack costB = rawOffer.getCostB().isEmpty() ? ItemStack.EMPTY : rawOffer.getCostB().copy();
            ItemStack result = rawOffer.getResult().copy();

            // Проверяем, нет ли уже точно такого же товара в списке
            boolean duplicate = false;
            for (MerchantOffer existing : offers) {
                if (ItemStack.isSameItemSameTags(existing.getResult(), result)) {
                    duplicate = true;
                    break;
                }
            }
            if (duplicate) continue;

            // Добавляем преобразованное предложение
            offers.add(new MerchantOffer(
                    coinCost,
                    costB,
                    result,
                    12, // Дневной лимит покупок этого лота
                    0,  // Без опыта жителю
                    0.0F
            ));
        }

        return offers;
    }
}
