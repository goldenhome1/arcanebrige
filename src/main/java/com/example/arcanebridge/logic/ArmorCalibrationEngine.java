package com.example.arcanebridge.logic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class ArmorCalibrationEngine {
    private static final Random random = new Random();

    /**
     * Поломка СТРОГО элемента брони с механической частотой
     */
    public static void jamRandomArmorPiece(ServerPlayer player) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        List<ItemStack> candidates = new ArrayList<>();

        for (EquipmentSlot slot : slots) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                String freq = ResonanceEngine.getEquipmentFrequency(stack);
                boolean isAlreadyJammed = stack.hasTag() && stack.getTag().getBoolean("Jammed");

                if ("mechanical".equals(freq) && !isAlreadyJammed) {
                    candidates.add(stack);
                }
            }
        }

        if (!candidates.isEmpty()) {
            ItemStack chosen = candidates.get(random.nextInt(candidates.size()));
            chosen.getOrCreateTag().putBoolean("Jammed", true);
            player.sendSystemMessage(Component.literal("§c§l[⚙ ПЕРЕГРУЗКА]: Сервоприводы элемента [" + chosen.getHoverName().getString() + "§c§l] заклинили! Защита отключена."));
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 0.6f);
        }
    }

    /**
     * Обнуление всех статов заклинившей брони
     */
    @SubscribeEvent
    public static void onItemAttributeModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.hasTag() && stack.getTag().getBoolean("Jammed")) {
            event.clearModifiers();
        }
    }

    /**
     * Блокировка надевания заклинившей брони по ПКМ
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onArmorRightClick(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (stack.hasTag() && stack.getTag().getBoolean("Jammed")) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    /**
     * Перехват клика ключом в воздух
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        handleWrenchClick(event.getEntity(), event.getHand(), event);
    }

    /**
     * Перехват клика ключом по блоку (предотвращает конфликт с механизмами Create)
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        handleWrenchClick(event.getEntity(), event.getHand(), event);
    }

    private static void handleWrenchClick(Player player, InteractionHand hand, PlayerInteractEvent event) {
        if (hand != InteractionHand.MAIN_HAND || player == null) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        ResourceLocation mainItemId = ForgeRegistries.ITEMS.getKey(mainHand.getItem());
        boolean isWrench = mainItemId != null && mainItemId.toString().equals("create:wrench");
        boolean isJammed = offHand.hasTag() && offHand.getTag().getBoolean("Jammed");

        if (isWrench && isJammed) {
            if (player.level().isClientSide()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.example.arcanebridge.client.gui.ClientArmorGuiHelper.openRepairScreen();
                });
            }
            player.swing(InteractionHand.MAIN_HAND, true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}