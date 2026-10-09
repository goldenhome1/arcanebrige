package com.example.arcanebridge.logic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class CasterResonanceHandler {

    private static final Pattern GLYPH_PATTERN = Pattern.compile("ars_[a-z]+:glyph_[a-z0-9_]+");

    /**
     * Вызов кратковременного частотного перегруза (+1..2 виртуальных элемента брони)
     */
    private static void triggerSurge(ServerPlayer player, ItemStack stack, int virtualArmorLoad, int durationTicks, String triggerSource) {
        if (stack.isEmpty()) return;

        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null || !rl.getNamespace().startsWith("ars_")) return;

        CompoundTag pData = player.getPersistentData();
        long currentTick = player.level().getGameTime();

        // Защита от дублирования импульса в одном тике
        if (pData.getLong("last_caster_surge_tick") == currentTick) return;
        pData.putLong("last_caster_surge_tick", currentTick);

        // 1. Извлекаем реальный список глифов текущего заклинания
        List<String> glyphs = extractActiveGlyphs(stack);
        boolean isElemental = isElementalSpell(glyphs);

        String freqName = isElemental ? "ELEMENTAL" : "ARCANE";
        String timerKey = isElemental ? "ele_shock_timer" : "arcane_shock_timer";
        String loadKey = isElemental ? "ele_caster_load" : "arcane_caster_load";

        // 2. Взводим виртуальную нагрузку в NBT игрока (не отнимая стабильность напрямую!)
        pData.putInt(timerKey, durationTicks);
        pData.putInt(loadKey, virtualArmorLoad);

        // 3. Отладочный отчет в чат
        Component debugMsg = Component.literal(
                "§6§l[DEBUG CASTER] §aТриггер: §e" + triggerSource +
                "\n §7➤ Оружие: §b" + rl +
                "\n §7➤ Глифы: §f" + (glyphs.isEmpty() ? "[Базовый фокус]" : glyphs) +
                "\n §7➤ Спектр: " + (isElemental ? "§bСтихийный (ELE)" : "§dАрканный (ARCANE)") +
                "\n §7➤ Частотный импульс: §c+" + virtualArmorLoad + " к нагрузке " + freqName + " §7(на " + (durationTicks / 20.0f) + " сек)"
        );
        player.sendSystemMessage(debugMsg);

        // 4. Аудио-визуальный отклик выстрела
        ServerLevel level = player.serverLevel();
        double px = player.getX();
        double py = player.getY() + 1.1D;
        double pz = player.getZ();

        if (isElemental) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 8, 0.2D, 0.2D, 0.2D, 0.04D);
            level.playSound(null, px, py, pz, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7F, 1.3F);
        } else {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT, px, py, pz, 12, 0.25D, 0.25D, 0.25D, 0.08D);
            level.playSound(null, px, py, pz, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8F, 1.4F);
        }
    }

    /**
     * Точное извлечение идентификаторов глифов только из активного заклинания
     */
    private static List<String> extractActiveGlyphs(ItemStack stack) {
        List<String> glyphs = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null) return glyphs;

        CompoundTag spellData = tag.getCompound("ars_nouveau_spell");
        if (spellData.isEmpty()) spellData = tag.getCompound("spell");

        int currentSlot = spellData.getInt("current_slot");
        CompoundTag slotTag = spellData.getCompound("spells" + currentSlot);
        if (slotTag.isEmpty()) slotTag = spellData;

        // Вариант 1: Стандартный ListTag "spell"
        if (slotTag.contains("spell", Tag.TAG_LIST)) {
            ListTag list = slotTag.getList("spell", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                glyphs.add(list.getString(i).toLowerCase());
            }
        }

        // Вариант 2: Точный поиск сигнатур глифов через Regex
        if (glyphs.isEmpty()) {
            Matcher matcher = GLYPH_PATTERN.matcher(slotTag.toString().toLowerCase());
            while (matcher.find()) {
                glyphs.add(matcher.group());
            }
        }

        return glyphs;
    }

    /**
     * Проверка: содержит ли заклинание истинно стихийные глифы
     */
    private static boolean isElementalSpell(List<String> glyphs) {
        for (String g : glyphs) {
            // Любой глиф аддона Ars Elemental
            if (g.startsWith("ars_elemental:")) return true;

            // Стихийные глифы базового Ars Nouveau (без ложных совпадений с repair/air)
            if (g.contains("glyph_flare") || g.contains("glyph_ignite") || g.contains("glyph_firework")
             || g.contains("glyph_freeze") || g.contains("glyph_cold_snap") || g.contains("glyph_conjure_water")
             || g.contains("glyph_crush")
             || g.contains("glyph_wind") || g.contains("glyph_gust") || g.contains("glyph_launch")
             || g.contains("glyph_lightning")) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // ПЕРЕХВАТЧИКИ СОБЫТИЙ
    // =========================================================================

    // 1. Лук чародея (отпускание тетивы) — +2 виртуальной брони на 3 секунды
    @SubscribeEvent
    public static void onUseItemStop(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = event.getItem();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("bow") && !rl.getPath().contains("crossbow")) {
                triggerSurge(player, stack, 2, 60, "LivingEntityUseItemEvent.Stop");
            }
        }
    }

    // 2. Ванильный хук лука
    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack bow = event.getBow();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(bow.getItem());
            if (rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("bow")) {
                triggerSurge(player, bow, 2, 60, "ArrowLooseEvent");
            }
        }
    }

    // 3. Вылет стрелы в мир
    @SubscribeEvent
    public static void onProjectileSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof ServerPlayer player) {
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            ItemStack weapon = isArsBow(mainHand) ? mainHand : (isArsBow(offHand) ? offHand : ItemStack.EMPTY);

            if (!weapon.isEmpty()) {
                triggerSurge(player, weapon, 2, 60, "EntityJoinLevelEvent(Arrow)");
            }
        }
    }

    // 4. Удар мечом чародея — +1 виртуальной брони на 2 секунды
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack held = player.getMainHandItem();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(held.getItem());
            if (rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("sword")) {
                triggerSurge(player, held, 1, 40, "AttackEntityEvent");
            }
        }
    }

    // 5. Арбалет (+2) и Жезл/Зеркало (+1)
    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = event.getItemStack();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (rl == null || !rl.getNamespace().startsWith("ars_")) return;

            String path = rl.getPath();
            if (path.contains("crossbow") && CrossbowItem.isCharged(stack)) {
                triggerSurge(player, stack, 2, 60, "RightClickItem(Crossbow)");
            } else if (path.contains("wand") || path.contains("mirror")) {
                triggerSurge(player, stack, 1, 40, "RightClickItem(Wand/Mirror)");
            }
        }
    }

    private static boolean isArsBow(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("bow") && !rl.getPath().contains("crossbow");
    }
}