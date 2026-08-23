package com.example.arcanebridge.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class CaveMobSpawnHandler {

    private static final Set<ResourceLocation> CAVE_MOBS = Set.of(
            new ResourceLocation("alexscaves", "notor"),
            new ResourceLocation("alexscaves", "ferrouslime"),
            new ResourceLocation("alexscaves", "boundroid"),
            new ResourceLocation("alexscaves", "teletor")
    );

    @SubscribeEvent
    public static void onCheckSpawnPosition(MobSpawnEvent.PositionCheck event) {
        if (event.getEntity() == null) return;

        EntityType<?> entityType = event.getEntity().getType();
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entityType);

        if (entityId != null && CAVE_MOBS.contains(entityId)) {
            LevelAccessor level = event.getLevel();
            BlockPos pos = new BlockPos((int) event.getX(), (int) event.getY(), (int) event.getZ());

            // 1. Запрещаем спавн выше Y = 0 (только глубокие пещеры)
            if (pos.getY() >= 0) {
                event.setResult(Event.Result.DENY);
                return;
            }

            // 2. Блокируем спавн под открытым небом
            if (level.canSeeSky(pos)) {
                event.setResult(Event.Result.DENY);
            }
        }
    }
}