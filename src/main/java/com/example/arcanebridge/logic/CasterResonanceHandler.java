package com.example.arcanebridge.logic;

import com.example.arcanebridge.capability.ResonanceProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class CasterResonanceHandler {

    private static final Set<String> MONITORED_ITEMS = Set.of(
            "ars_nouveau:enchanters_sword",
            "ars_nouveau:enchanters_bow",
            "ars_nouveau:enchanters_crossbow",
            "ars_nouveau:wand",
            "ars_nouveau:enchanters_mirror"
    );

    /**
     * Основной метод импульсного эфирного всплеска
     */
    private static void triggerSurge(ServerPlayer player, ItemStack stack, float stabilityCost) {
        if (player.isCreative() || stack.isEmpty()) return;

        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null || !MONITORED_ITEMS.contains(rl.toString())) return;

        CompoundTag pData = player.getPersistentData();
        long currentTick = player.level().getGameTime();

        // Защита от двойного списания в один игровой такт
        if (pData.getLong("last_caster_surge_tick") == currentTick) return;
        pData.putLong("last_caster_surge_tick", currentTick);

        // 1. Определение типа всплеска (стихийный или арканный)
        boolean isElemental = isElementalSpell(stack);
        String timerKey = isElemental ? "ele_shock_timer" : "arcane_shock_timer";

        // 2. Списание стабильности через капабилити ResonanceProvider
        player.getCapability(ResonanceProvider.RESONANCE).ifPresent(res -> {
            res.addStability(-stabilityCost);
        });

        // 3. Активация таймера тревоги HUD на 2 секунды (40 тиков)
        // ResonanceEngine.java автоматически уменьшает этот таймер на 10 каждые 10 тиков
        pData.putInt(timerKey, 40);

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

    /**
     * Анализ NBT-матрицы заклинания на наличие стихийных дескрипторов
     */
    private static boolean isElementalSpell(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return false;
        String nbt = tag.toString().toLowerCase();

        return nbt.contains("elemental") ||
               nbt.contains("flare") ||
               nbt.contains("fire") ||
               nbt.contains("ignis") ||
               nbt.contains("freeze") ||
               nbt.contains("water") ||
               nbt.contains("crush") ||
               nbt.contains("earth") ||
               nbt.contains("air") ||
               nbt.contains("wind");
    }

    // =========================================================================
    // ПЕРЕХВАТ СОБЫТИЙ ОРУЖИЯ И ФОКУСОВ
    // =========================================================================

    // 1. Выстрел из Лука чародея (-8% стабильности)
    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            triggerSurge(player, event.getBow(), 8.0F);
        }
    }

    // 2. Взмах Мечом чародея (-5% стабильности)
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            triggerSurge(player, player.getMainHandItem(), 5.0F);
        }
    }

    // 3. Выстрел из заряженного Арбалета, Жезла или Зеркала (-6%..-8%)
    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = event.getItemStack();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (rl == null) return;

            String itemId = rl.toString();
            if (itemId.equals("ars_nouveau:enchanters_crossbow") && CrossbowItem.isCharged(stack)) {
                triggerSurge(player, stack, 8.0F);
            } else if (itemId.equals("ars_nouveau:wand") || itemId.equals("ars_nouveau:enchanters_mirror")) {
                triggerSurge(player, stack, 6.0F);
            }
        }
    }
}