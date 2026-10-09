package com.example.arcanebridge.logic;

import com.example.arcanebridge.capability.ResonanceProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class CasterResonanceHandler {

    /**
     * Основной импульс эфирного всплеска
     */
    private static void triggerSurge(ServerPlayer player, ItemStack stack, float stabilityCost) {
        if (stack.isEmpty()) return;

        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null || !rl.getNamespace().equals("ars_nouveau")) return;

        CompoundTag pData = player.getPersistentData();
        long currentTick = player.level().getGameTime();

        // Защита от дублирования импульса в один и тот же игровой такт
        if (pData.getLong("last_caster_surge_tick") == currentTick) return;
        pData.putLong("last_caster_surge_tick", currentTick);

        // 1. Определение типа энергии (стихийная или чистая аркана)
        boolean isElemental = isElementalSpell(stack);
        String timerKey = isElemental ? "ele_shock_timer" : "arcane_shock_timer";

        // 2. Списание стабильности через Capability
        final float[] updatedStability = new float[]{100.0f};
        player.getCapability(ResonanceProvider.RESONANCE).ifPresent(res -> {
            res.addStability(-stabilityCost);
            updatedStability[0] = res.getStability();
        });

        // 3. Взвод таймера тревоги HUD (40 тиков = 2 секунды мигания буквы A или E)
        pData.putInt(timerKey, 40);

        // 4. Мгновенная индикация в Actionbar над хотбаром для наглядного теста
        String typeColor = isElemental ? "§b[Стихийный всплеск]" : "§d[Арканный всплеск]";
        player.displayClientMessage(
                Component.literal(typeColor + " §c-" + (int)stabilityCost + "% стабильности §7| Остаток: §e" + String.format(java.util.Locale.US, "%.1f", updatedStability[0]) + "%"),
                true
        );

        // 5. Визуальный и аудио отклик
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
     * Сканирование NBT заклинания на стихийные глифы
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
    // ПЕРЕХВАТЧИКИ СОБЫТИЙ
    // =========================================================================

    // 1. Выстрел из Лука чародея (Перехват отпускания тетивы)
    @SubscribeEvent
    public static void onUseItemStop(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = event.getItem();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (rl == null) return;

            String path = rl.getPath();
            // Отсекаем арбалет, берем только луки Ars Nouveau
            if (rl.getNamespace().equals("ars_nouveau") && path.contains("bow") && !path.contains("crossbow")) {
                int pullTicks = stack.getUseDuration() - event.getDuration();
                // Натяжение тетивы минимум 5 тиков (отсекаем случайные миссклики)
                if (pullTicks >= 5) {
                    triggerSurge(player, stack, 8.0F);
                }
            }
        }
    }

    // 2. Удар Мечом чародея
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack held = player.getMainHandItem();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(held.getItem());
            if (rl != null && rl.getNamespace().equals("ars_nouveau") && rl.getPath().contains("sword")) {
                triggerSurge(player, held, 5.0F);
            }
        }
    }

    // 3. Выстрел из заряженного Арбалета, Жезла или Зеркала
    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = event.getItemStack();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (rl == null || !rl.getNamespace().equals("ars_nouveau")) return;

            String path = rl.getPath();
            if (path.contains("crossbow") && CrossbowItem.isCharged(stack)) {
                triggerSurge(player, stack, 8.0F);
            } else if (path.contains("wand") || path.contains("mirror")) {
                triggerSurge(player, stack, 6.0F);
            }
        }
    }
}