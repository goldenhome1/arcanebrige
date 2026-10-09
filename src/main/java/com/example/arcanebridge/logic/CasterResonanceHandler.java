package com.example.arcanebridge.logic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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

    private static final Pattern GLYPH_PATTERN = Pattern.compile("([a-z0-9_]+:)?glyph_[a-z0-9_]+");

    /**
     * Вызов импульса: +0.5 к нагрузке на 5 секунд (100 тиков)
     */
    private static void triggerSurge(ServerPlayer player, ItemStack stack, float virtualArmorLoad, int durationTicks) {
        if (stack.isEmpty()) return;

        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null || !rl.getNamespace().startsWith("ars_")) return;

        // 1. Проверяем наличие глифов в оружии
        List<String> glyphs = extractActiveGlyphs(stack);
        if (glyphs.isEmpty()) {
            return; // Оружие без магии не трогает резонанс
        }

        CompoundTag pData = player.getPersistentData();
        long currentTick = player.level().getGameTime();

        // Защита от дублирования импульса в одном тике
        if (pData.getLong("last_caster_surge_tick") == currentTick) return;
        pData.putLong("last_caster_surge_tick", currentTick);

        // 2. Определение стихии/арканы
        boolean isElemental = isElementalSpell(glyphs);
        String timerKey = isElemental ? "ele_shock_timer" : "arcane_shock_timer";
        String loadKey = isElemental ? "ele_caster_load" : "arcane_caster_load";

        // 3. Расчет нагрузки: каждый выстрел/удар строго +0.5F
        int currentTimer = pData.getInt(timerKey);
        float currentLoad = currentTimer > 0 ? pData.getFloat(loadKey) : 0.0F;
        float newLoad = currentLoad + virtualArmorLoad;

        pData.putInt(timerKey, durationTicks);
        pData.putFloat(loadKey, newLoad);

        // 4. Локальный визуальный и аудио-отклик
        ServerLevel level = player.serverLevel();
        double px = player.getX();
        double py = player.getY() + 1.1D;
        double pz = player.getZ();

        if (isElemental) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 10, 0.25D, 0.25D, 0.25D, 0.04D);
            level.playSound(null, px, py, pz, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7F, 1.3F);
        } else {
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, px, py, pz, 14, 0.3D, 0.3D, 0.3D, 0.08D);
            level.playSound(null, px, py, pz, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8F, 1.4F);
        }
    }

    private static List<String> extractActiveGlyphs(ItemStack stack) {
        List<String> glyphs = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null) return glyphs;

        String nbt = tag.toString().toLowerCase();
        Matcher matcher = GLYPH_PATTERN.matcher(nbt);
        while (matcher.find()) {
            String g = matcher.group();
            if (!glyphs.contains(g)) {
                glyphs.add(g);
            }
        }
        return glyphs;
    }

    private static boolean isElementalSpell(List<String> glyphs) {
        for (String g : glyphs) {
            String lower = g.toLowerCase();
            if (lower.contains("ars_elemental")) return true;

            if (lower.contains("glyph_flare") || lower.contains("glyph_ignite") || lower.contains("glyph_firework")
             || lower.contains("glyph_freeze") || lower.contains("glyph_cold_snap") || lower.contains("glyph_conjure_water")
             || lower.contains("glyph_crush")
             || lower.contains("glyph_wind") || lower.contains("glyph_gust") || lower.contains("glyph_launch")
             || lower.contains("glyph_lightning")) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // ПЕРЕХВАТЧИКИ СОБЫТИЙ (ЛУК, АРБАЛЕТ, МЕЧ)
    // =========================================================================

    // 1. Выстрел из Лука чародея (Stop using) — 100 тиков (5 секунд), +0.5 нагрузки
    @SubscribeEvent
    public static void onUseItemStop(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = event.getItem();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("bow") && !rl.getPath().contains("crossbow")) {
                triggerSurge(player, stack, 0.5F, 100);
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
                triggerSurge(player, bow, 0.5F, 100);
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
                triggerSurge(player, weapon, 0.5F, 100);
            }
        }
    }

    // 4. Удар Мечом чародея (+0.5 на 5 секунд)
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack held = player.getMainHandItem();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(held.getItem());
            if (rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("sword")) {
                triggerSurge(player, held, 0.5F, 100);
            }
        }
    }

    // 5. Выстрел из заряженного Арбалета (+0.5 на 5 секунд)
    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = event.getItemStack();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (rl == null || !rl.getNamespace().startsWith("ars_")) return;

            String path = rl.getPath();
            if (path.contains("crossbow") && CrossbowItem.isCharged(stack)) {
                triggerSurge(player, stack, 0.5F, 100);
            }
        }
    }

    private static boolean isArsBow(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("bow") && !rl.getPath().contains("crossbow");
    }
}