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

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class CasterResonanceHandler {

    /**
     * Основной импульс эфирного всплеска со списанием и выводом отладки в чат
     */
    private static void triggerSurge(ServerPlayer player, ItemStack stack, float stabilityCost, String triggerSource) {
        if (stack.isEmpty()) return;

        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null) return;
        String itemId = rl.toString();

        // Проверяем принадлежность к Ars Nouveau или Ars Elemental
        if (!rl.getNamespace().startsWith("ars_")) return;

        CompoundTag pData = player.getPersistentData();
        long currentTick = player.level().getGameTime();

        // Защита от дублирования в рамках одного тика
        if (pData.getLong("last_caster_surge_tick") == currentTick) return;
        pData.putLong("last_caster_surge_tick", currentTick);

        // 1. Определение типа магии (Стихии vs Аркана)
        boolean isElemental = isElementalSpell(stack);
        String timerKey = isElemental ? "ele_shock_timer" : "arcane_shock_timer";

        // 2. Списание стабильности через Capability
        final float[] oldStability = new float[]{100.0f};
        final float[] newStability = new float[]{100.0f};

        player.getCapability(ResonanceProvider.RESONANCE).ifPresent(res -> {
            oldStability[0] = res.getStability();
            res.addStability(-stabilityCost);
            newStability[0] = res.getStability();
        });

        // 3. Взвод таймера шока для визора (40 тиков = 2 секунды)
        pData.putInt(timerKey, 40);

        // 4. Отладочный вывод прямо в чат игроку (Actionbar удален)
        CompoundTag stackTag = stack.getTag();
        String nbtPreview = (stackTag != null) ? stackTag.toString() : "NBT отсутствует";
        if (nbtPreview.length() > 60) {
            nbtPreview = nbtPreview.substring(0, 57) + "...";
        }

        Component debugMsg = Component.literal(
                "§6§l[DEBUG CASTER] §aСработал триггер: §e" + triggerSource +
                "\n §7➤ Предмет: §b" + itemId +
                "\n §7➤ Тип энергии: " + (isElemental ? "§bСтихийный (ELE)" : "§dАрканный (ARCANE)") +
                "\n §7➤ Стабильность: §c" + String.format(java.util.Locale.US, "%.1f", oldStability[0]) + "% §7➔ §a" + String.format(java.util.Locale.US, "%.1f", newStability[0]) + "% (-" + (int)stabilityCost + "%)" +
                "\n §7➤ Матрица NBT: §8" + nbtPreview
        );
        player.sendSystemMessage(debugMsg);

        // Дублирование в серверную консоль
        System.out.println(String.format(
                "[ARCANE-CASTER-DEBUG] Player '%s' used '%s' via %s. Energy: %s. Stability: %.1f -> %.1f",
                player.getName().getString(), itemId, triggerSource,
                isElemental ? "ELE" : "ARCANE", oldStability[0], newStability[0]
        ));

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
     * Сканирование NBT-матрицы заклинания
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
    // ПЕРЕХВАТЧИКИ СОБЫТИЙ (ЛОВИМ ВСЕ КАНАЛЫ)
    // =========================================================================

    // Канал 1: Ванильный хук отпускания тетивы лука
    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack bow = event.getBow();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(bow.getItem());
            if (rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("bow")) {
                triggerSurge(player, bow, 8.0F, "ArrowLooseEvent");
            }
        }
    }

    // Канал 2: Общий останов использования предмета (LivingEntity.releaseUsingItem)
    @SubscribeEvent
    public static void onUseItemStop(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = event.getItem();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (rl != null && rl.getNamespace().startsWith("ars_")) {
                String path = rl.getPath();
                if (path.contains("bow") && !path.contains("crossbow")) {
                    triggerSurge(player, stack, 8.0F, "LivingEntityUseItemEvent.Stop");
                }
            }
        }
    }

    // Канал 3: Прямой спавн стрелы или снаряда в мире стрелком
    @SubscribeEvent
    public static void onProjectileSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof AbstractArrow arrow) {
            if (arrow.getOwner() instanceof ServerPlayer player) {
                ItemStack mainHand = player.getMainHandItem();
                ItemStack offHand = player.getOffhandItem();
                ItemStack weapon = isArsBow(mainHand) ? mainHand : (isArsBow(offHand) ? offHand : ItemStack.EMPTY);

                if (!weapon.isEmpty()) {
                    triggerSurge(player, weapon, 8.0F, "EntityJoinLevelEvent(Arrow)");
                }
            }
        }
    }

    // Канал 4: Удар Мечом чародея
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack held = player.getMainHandItem();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(held.getItem());
            if (rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("sword")) {
                triggerSurge(player, held, 5.0F, "AttackEntityEvent");
            }
        }
    }

    // Канал 5: Клик ПКМ (Арбалет, Жезл, Зеркало)
    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = event.getItemStack();
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (rl == null || !rl.getNamespace().startsWith("ars_")) return;

            String path = rl.getPath();
            if (path.contains("crossbow") && CrossbowItem.isCharged(stack)) {
                triggerSurge(player, stack, 8.0F, "RightClickItem(Crossbow)");
            } else if (path.contains("wand") || path.contains("mirror")) {
                triggerSurge(player, stack, 6.0F, "RightClickItem(Wand/Mirror)");
            }
        }
    }

    private static boolean isArsBow(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return rl != null && rl.getNamespace().startsWith("ars_") && rl.getPath().contains("bow") && !rl.getPath().contains("crossbow");
    }
}