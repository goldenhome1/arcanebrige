package com.example.arcanebridge.event;

import com.example.arcanebridge.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class BossDropHandler {

        private static final ResourceLocation UMVUTHI_ID = new ResourceLocation("mowziesmobs", "umvuthi");
    private static final ResourceLocation FROSTMAW_ID = new ResourceLocation("mowziesmobs", "frostmaw");

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        ResourceLocation entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityKey == null) return;

        if (entityKey.equals(UMVUTHI_ID)) {
            ItemStack dropStack = new ItemStack(ModItems.ANCIENT_SCROLL_PHASE.get());
            ItemEntity itemEntity = new ItemEntity(
                    entity.level(),
                    entity.getX(), entity.getY() + 0.5D, entity.getZ(),
                    dropStack
            );
            itemEntity.setDefaultPickUpDelay();
            event.getDrops().add(itemEntity);
        } else if (entityKey.equals(FROSTMAW_ID)) {
            ItemStack dropStack = new ItemStack(ModItems.ANCIENT_SCROLL_FLUID.get());
            ItemEntity itemEntity = new ItemEntity(
                    entity.level(),
                    entity.getX(), entity.getY() + 0.5D, entity.getZ(),
                    dropStack
            );
            itemEntity.setDefaultPickUpDelay();
            event.getDrops().add(itemEntity);
        }
    }
}