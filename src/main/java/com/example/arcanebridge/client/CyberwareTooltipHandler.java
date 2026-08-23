package com.example.arcanebridge.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = "arcane_bridge", value = Dist.CLIENT)
public class CyberwareTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (!itemId.startsWith("cyber_ware_port:")) return;

        List<Component> tooltip = event.getToolTip();
        boolean shiftDown = Screen.hasShiftDown();
        boolean ctrlDown = Screen.hasControlDown();

        // 1. Состояние покоя (ни одна клавиша не зажата) — выводим подсказки
        if (!shiftDown && !ctrlDown) {
            tooltip.add(Component.literal("§8[Зажмите Shift для описания Cyberware]"));
            tooltip.add(Component.literal("§8[Зажмите Ctrl для протокола интеграции]"));
            return;
        }

        // 2. Если зажат только Shift — Cyberware выводит стандартный текст
        if (shiftDown && !ctrlDown) {
            tooltip.add(Component.literal("§8[Зажмите Ctrl для протокола интеграции]"));
            return;
        }

        // 3. Если зажат Ctrl — спецификация интеграции
        if (ctrlDown) {
            tooltip.add(Component.literal("§6§l⚙ СПЕЦИФИКАЦИЯ МОДИФИКАЦИИ:"));

            switch (itemId) {
                case "cyber_ware_port:cybereye_upgrades_hudjack" -> {
                    tooltip.add(Component.literal(" §b📊 Тактический нейро-хаб [HUD Jack]"));
                    tooltip.add(Component.literal("  §7• Разблокирует вывод проекционных систем на сетчатку:"));
                    tooltip.add(Component.literal("    §f- Оптическая карта и миникарта §7[Xaero's Map]"));
                    tooltip.add(Component.literal("    §f- Сканер HP мобов и полоски здоровья §7[Neat]"));
                    tooltip.add(Component.literal("    §f- Индикация нанесённого урона §7[Damage Numbers]"));
                    tooltip.add(Component.literal("    §f- Анализатор блоков и механизмов §7[Jade / Create]"));
                    tooltip.add(Component.literal("    §f- Монитор частот §7[Эфирного Резонанса]"));
                    tooltip.add(Component.literal("  §7• Меню настройки и отключения слоев по клавише §f[H]§7."));
                }

                case "cyber_ware_port:muscle_upgrades_wired_reflexes" -> {
                    tooltip.add(Component.literal(" §b⚡ Протокол «Нейро-парирование» [Just Parry]"));
                    tooltip.add(Component.literal("  §7• Разблокирует парирование атак и снарядов по клавише §f[G]§7."));
                    tooltip.add(Component.literal("  §7• Калиброванное окно реакции: §a5 тиков§7."));
                    tooltip.add(Component.literal("  §8(Без импланта парирование недоступно)"));
                }

                case "cyber_ware_port:hand_upgrades_mining" -> {
                    tooltip.add(Component.literal(" §b⛏ Кинетический бур жил [FTB Ultimine]"));
                    tooltip.add(Component.literal("  §7• Разрешает цепной майнинг жил голыми руками и инструментом."));
                    tooltip.add(Component.literal("  §e• Расход: §f10 FE за блок §7(без истощения шкалы голода)."));
                    tooltip.add(Component.literal("  §8(Без импланта или питания майнинг жил блокируется)"));
                }

                case "cyber_ware_port:cybereye_upgrades_zoom" -> {
                    tooltip.add(Component.literal(" §b🔍 Оптический фокус [Just Zoom]"));
                    tooltip.add(Component.literal("  §7• Разблокирует оптическое приближение по назначенной клавише."));
                    tooltip.add(Component.literal("  §7• Ванильный Shift-зум вырезан во избежание сбоев камеры."));
                    tooltip.add(Component.literal("  §8(Без импланта клавиша зума блокируется сбоем)"));
                }

                case "cyber_ware_port:arm_upgrades_bow" -> {
                    tooltip.add(Component.literal(" §b🎯 Баллистический синхронизатор руки"));
                    tooltip.add(Component.literal("  §7• Базово: увеличивает скорость натяжения тетивы луков."));
                    tooltip.add(Component.literal("  §a• Связка [Кибер-глаза + Маховик руки]:"));
                    tooltip.add(Component.literal("    §f- Проецирует голографический маркер попадания [ + ]."));
                    tooltip.add(Component.literal("    §f- Поддерживает огнестрел Create Gunsmithing, пушки CBC и луки."));
                }

                case "cyber_ware_port:cybereyes" -> {
                    tooltip.add(Component.literal(" §b👁 Оптический хост-комплекс"));
                    tooltip.add(Component.literal("  §7• Базовая хост-платформа для глазных модулей."));
                    tooltip.add(Component.literal("  §a• В связке со Скорострельным маховиком руки §7активирует"));
                    tooltip.add(Component.literal("    §7баллистический тактический маркер попадания на сетчатке."));
                }

                case "cyber_ware_port:bone_upgrades_boneflex" -> {
                    tooltip.add(Component.literal(" §b🦴 Демпфирующие полимеры [Create]"));
                    tooltip.add(Component.literal("  §7• Полный 100% иммунитет к кинетическому урону от сдавливания."));
                    tooltip.add(Component.literal("  §7• Защищает от дробильных колес и механических прессов."));
                }

                case "cyber_ware_port:heart_upgrades_platelets" -> {
                    tooltip.add(Component.literal(" §b🩸 Синтетический коагулянт [Majrusz's]"));
                    tooltip.add(Component.literal("  §7• Полный иммунитет к глубокому кровотечению §c[Bleeding]§7."));
                    tooltip.add(Component.literal("  §7• Мгновенно купирует открытые травмы при их возникновении."));
                }

                case "cyber_ware_port:lower_organs_upgrades_adrenaline" -> {
                    tooltip.add(Component.literal(" §b💉 Авто-инъектор берсерка [Better Combat]"));
                    tooltip.add(Component.literal("  §7• При здоровье ниже §c30%§7 активирует режим берсерка на §a5 сек§7."));
                    tooltip.add(Component.literal("  §7• Скорость серии атак: §a+40%§7 + эффект Спешки."));
                    tooltip.add(Component.literal("  §8• Время перезарядки: 45 секунд."));
                }

                default -> {
                    tooltip.add(Component.literal(" §7• Стандартный модуль аугментации Cyberware."));
                }
            }
        }
    }
}