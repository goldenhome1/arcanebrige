package com.example.arcanebridge.logic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class CgsExplosionHandler {

    private record PendingExplosion(ServerLevel level, Entity entity, Vec3 pos, float power) {}

    private static final List<PendingExplosion> EXPLOSION_QUEUE = new ArrayList<>();
    private static boolean isProcessing = false;

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        Level level = event.getLevel();

        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (isProcessing) {
            return;
        }

        Entity entity = event.getEntity();
        Entity.RemovalReason reason = entity.getRemovalReason();

        // Игнорируем выгрузку чанков и игрока
        if (reason == Entity.RemovalReason.UNLOADED_TO_CHUNK || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
            return;
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityId == null) {
            return;
        }

        String namespace = entityId.getNamespace();
        // Фильтруем только сущности CGS и NTGL
        if (!"cgs".equals(namespace) && !"ntgl".equals(namespace)) {
            return;
        }

        String path = entityId.getPath().toLowerCase();

        // 1. ИСКЛЮЧЕНИЕ ОБЫЧНЫХ ПУЛЬ: отсекаем стандартные снаряды, пули и дробь
        if (path.equals("projectile") || path.equals("bullet") || path.contains("bullet") || path.contains("pellet") || path.contains("shot") || path.contains("casing")) {
            return;
        }

        float power = 0.0f;

        // 2. Взрываем ТОЛЬКО специализированные взрывные боеприпасы
        if (path.contains("rocket_small") || path.contains("small_rocket")) {
            power = 0.7f; // Микро-ракета (компактный хлопок ~0.2 TNT)
        } else if (path.contains("rocket") || path.contains("missile")) {
            power = 2.5f; // Тяжелая ракета
        } else if (path.contains("grenade") || path.contains("40mm") || path.contains("bomb") || path.contains("mortar") || path.contains("explosive")) {
            power = 0.5f; // Граната / подствольник / мина
        }

        if (power > 0.0f) {
            Vec3 motion = entity.getDeltaMovement();
            Vec3 pos = entity.position();

            // Отступаем назад по вектору полёта, чтобы взрыв не поглощался текстурами
            if (motion.lengthSqr() > 0.001) {
                pos = pos.subtract(motion.normalize().scale(0.3));
            }

            int chunkX = ((int) pos.x) >> 4;
            int chunkZ = ((int) pos.z) >> 4;

            if (serverLevel.hasChunk(chunkX, chunkZ)) {
                synchronized (EXPLOSION_QUEUE) {
                    EXPLOSION_QUEUE.add(new PendingExplosion(serverLevel, entity, pos, power));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        List<PendingExplosion> toProcess;
        synchronized (EXPLOSION_QUEUE) {
            if (EXPLOSION_QUEUE.isEmpty()) return;
            toProcess = new ArrayList<>(EXPLOSION_QUEUE);
            EXPLOSION_QUEUE.clear();
        }

        isProcessing = true;
        try {
            for (PendingExplosion exp : toProcess) {
                exp.level().explode(
                    exp.entity(),
                    exp.pos().x,
                    exp.pos().y,
                    exp.pos().z,
                    exp.power(),
                    Level.ExplosionInteraction.TNT
                );
            }
        } finally {
            isProcessing = false;
        }
    }
}