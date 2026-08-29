package com.example.arcanebridge.combat;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = MobArchetypes.MODID)
public class DamageDebugLogger {

        @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityHurtDebug(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;

        // Ищем игроков с командным блоком в хотбаре в радиусе 32 блоков
        java.util.List<ServerPlayer> debugWatchers = new java.util.ArrayList<>();
        for (ServerPlayer player : target.level().getServer().getPlayerList().getPlayers()) {
            if (player.distanceToSqr(target) <= 1024 && hasCommandBlockInHotbar(player)) {
                debugWatchers.add(player);
            }
        }

        // Если ни у кого рядом нет командного блока в хотбаре — полностью глушим дебаг
        if (debugWatchers.isEmpty()) return;

        DamageSource source = event.getSource();
        Entity trueSource = source.getEntity();
        Entity directEntity = source.getDirectEntity();

        // 1. Получаем точный DamageType ID из реестра 1.20.1
        String damageTypeId = "unknown";
        try {
            ResourceLocation key = target.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getKey(source.type());
            if (key != null) {
                damageTypeId = key.toString();
            }
        } catch (Exception ignored) {}

        // 2. Определяем сущность снаряда/атаки (Direct Entity)
        String directEntityName = "None (Direct)";
        if (directEntity != null) {
            ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(directEntity.getType());
            directEntityName = directEntity.getClass().getSimpleName() + " [" + (typeKey != null ? typeKey : "unknown") + "]";
        }

        // 3. Определяем инициатора урона (True Source: игрок, моб, пушка)
        String trueSourceName = "Environment / Block";
        String itemInHand = "N/A";
        if (trueSource != null) {
            ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(trueSource.getType());
            trueSourceName = trueSource.getClass().getSimpleName() + " [" + (typeKey != null ? typeKey : "unknown") + "]";
            if (trueSource instanceof LivingEntity livingSource) {
                ItemStack held = livingSource.getMainHandItem();
                itemInHand = held.isEmpty() ? "Empty Hand" : ForgeRegistries.ITEMS.getKey(held.getItem()).toString();
            }
        }

        String targetName = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();

        // 4. Формируем подробный вывод
        String logConsole = String.format(
                "[ARCANE-COMBAT-DEBUG] Target: '%s' (ID:%d) | DmgType: '%s' | MsgId: '%s' | Direct: '%s' | TrueSource: '%s' | Item: '%s' | Amount: %.2f",
                targetName, target.getId(), damageTypeId, source.getMsgId(), directEntityName, trueSourceName, itemInHand, event.getAmount()
        );
        System.out.println(logConsole);

                // 5. Отправляем в чат только тем игрокам, у которых есть командный блок в хотбаре
        Component chatMsg = Component.literal(
                "§6§l[DEBUG DMG] §f" + targetName + " §7<= §c" + damageTypeId +
                " §7| Msg: §e" + source.getMsgId() +
                " §7| Direct: §b" + directEntityName +
                " §7| Source: §a" + trueSourceName +
                " §7| Dmg: §d" + String.format(java.util.Locale.US, "%.1f", event.getAmount())
        );

        for (ServerPlayer player : debugWatchers) {
            player.sendSystemMessage(chatMsg);
        }
    }

    /**
     * Проверка наличия любого командного блока в слотах хотбара (0-8)
     */
    private static boolean hasCommandBlockInHotbar(ServerPlayer player) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                Item item = stack.getItem();
                if (item == Items.COMMAND_BLOCK || item == Items.CHAIN_COMMAND_BLOCK || item == Items.REPEATING_COMMAND_BLOCK) {
                    return true;
                }
            }
        }
        return false;
    }
}