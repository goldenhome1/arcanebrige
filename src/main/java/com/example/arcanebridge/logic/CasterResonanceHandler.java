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
     * Импульс выстрела: +1 к нагрузке частоты на 5 секунд (100 тиков)
     */
    private static void triggerSurge(ServerPlayer player, ItemStack stack, int durationTicks) {
        if (stack.isEmpty()) return;

        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null || !rl.getNamespace().startsWith("ars_")) return;

        // Игнорируем оружие без зашитых глифов
        List<String> glyphs = extractActiveGlyphs(stack);
        if (glyphs.isEmpty()) return;

        CompoundTag pData = player.getPersistentData();
        long currentTick = player.level().getGameTime();

        // Защита от дублирования импульса в рамках одного тика
        if (pData.getLong("last_caster_surge_tick") == currentTick) return;
        pData.putLong("last_caster_surge_tick", currentTick);

        // Определение спектра: стихийный или арканный
        boolean isElemental = isElementalSpell(glyphs);
        String timerKey = isElemental ? "ele_shock_timer" : "arcane_shock_timer";
        String loadKey = isElemental ? "ele_caster_load" : "arcane_caster_load";

        // Накопление нагрузки: каждый выстрел/удар строго +1 без потолка
        int currentTimer = pData.getInt(timerKey);
        int currentLoad = currentTimer > 0 ? pData.getInt(loadKey) : 0;
        int newLoad = currentLoad + 1;

        pData.putInt(timerKey, durationTicks);
        pData.putInt(loadKey, newLoad);

        // Визуальные и звуковые эффекты всплеска
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

    // 1. Выстрел из Лука чародея (отпускание тетивы)
    @SubscribeEvent
    public static void onUseItemStop(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = event.getItem();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("bow") && !rl.getPath().contains("crossbow")) {
                triggerSurge(player, stack, 100);
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
                triggerSurge(player, bow, 100);
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
                triggerSurge(player, weapon, 100);
            }
        }
    }

    // 4. Удар Мечом чародея
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack held = player.getMainHandItem();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(held.getItem());
            if (rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("sword")) {
                triggerSurge(player, held, 100);
            }
        }
    }

    // 5. Выстрел из заряженного Арбалета чародея
    @SubscribeEvent
    public static void onItemStopCrossbow(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = event.getItem();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("crossbow") && CrossbowItem.isCharged(stack)) {
                triggerSurge(player, stack, 100);
            }
        }
    }

    private static boolean isArsBow(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("bow") && !rl.getPath().contains("crossbow");
    }
}