package com.example.arcanebridge.combat;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = MobArchetypes.MODID)
public class DamageDebugLogger {

    // Слушаем событие с наивысшим приоритетом до применения всех срезов урона
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityHurtDebug(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        Entity trueSource = source.getEntity();
        Entity directEntity = source.getDirectEntity();
        LivingEntity target = event.getEntity();

        // Проверяем, что атаку проводит игрок (напрямую или через снаряд/заклинание)
        if (trueSource instanceof ServerPlayer player) {
            String damageTypeId = target.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getKey(source.type()) != null 
                    ? target.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getKey(source.type()).toString() 
                    : "unknown";

            String directEntityName = directEntity != null 
                    ? directEntity.getClass().getSimpleName() + " [" + ForgeRegistries.ENTITY_TYPES.getKey(directEntity.getType()) + "]" 
                    : "None (Direct/Indirect)";

            String targetName = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
            ItemStack heldItem = player.getMainHandItem();
            String itemInHand = heldItem.isEmpty() ? "Empty Hand" : ForgeRegistries.ITEMS.getKey(heldItem.getItem()).toString();

            // Формируем детальное сообщение в чат
            player.sendSystemMessage(Component.literal("§6§l[DEBUG DAMAGE]§r"));
            player.sendSystemMessage(Component.literal(" §e➤ Target: §f" + targetName + " §7(ID: " + target.getId() + ")"));
            player.sendSystemMessage(Component.literal(" §e➤ DamageType ID: §c" + damageTypeId));
            player.sendSystemMessage(Component.literal(" §e➤ MsgId: §c" + source.getMsgId()));
            player.sendSystemMessage(Component.literal(" §e➤ Direct Entity: §b" + directEntityName));
            player.sendSystemMessage(Component.literal(" §e➤ MainHand Item: §a" + itemInHand));
            player.sendSystemMessage(Component.literal(" §e➤ Raw Amount: §d" + event.getAmount()));

            // Вывод в консоль сервера
            System.out.println(String.format(
                    "[ARCANE-COMBAT-DEBUG] Player '%s' -> '%s' | DamageType: '%s' | MsgId: '%s' | DirectEntity: '%s' | Item: '%s' | Dmg: %.2f",
                    player.getName().getString(),
                    targetName,
                    damageTypeId,
                    source.getMsgId(),
                    directEntityName,
                    itemInHand,
                    event.getAmount()
            ));
        }
    }
}