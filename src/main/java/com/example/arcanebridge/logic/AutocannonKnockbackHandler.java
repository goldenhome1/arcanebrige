package com.example.arcanebridge.logic;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class AutocannonKnockbackHandler {

    // Сет боссов для сброса кинетики CBC в конце текущего серверного тика
    private static final Set<LivingEntity> PENDING_MOMENTUM_RESET = 
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    /**
     * 1. Ванильная отдача: срезаем силу до ровно 3% (0.03F)
     */
    @SubscribeEvent
    public static void onLivingKnockback(LivingKnockBackEvent event) {
        LivingEntity entity = event.getEntity();
        if (isBossOrHeavy(entity)) {
            // Оставляем ровно 3% от стандартного ванильного импульса
            event.setStrength(event.getStrength() * 0.03f);
        }
    }

    /**
     * 2. Перехват урона артиллерии CBC / огнестрела
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;

        if (!isBossOrHeavy(victim)) return;

        var source = event.getSource();
        if (source.getDirectEntity() != null) {
            ResourceLocation projId = ForgeRegistries.ENTITY_TYPES.getKey(source.getDirectEntity().getType());
            if (projId != null) {
                String path = projId.getPath();
                String namespace = projId.getNamespace();
                
                if (namespace.equals("createbigcannons") || path.contains("autocannon") || path.contains("cannon") || namespace.equals("cgs")) {
                    PENDING_MOMENTUM_RESET.add(victim);
                }
            }
        }
    }

    /**
     * 3. Серверный сброс (END Phase): срезаем кинетику AP-снарядов CBC до 3% по горизонтали
     * и на 100% блокируем любые попытки подбросить босса вверх (+Y).
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_MOMENTUM_RESET.isEmpty()) return;

        synchronized (PENDING_MOMENTUM_RESET) {
            for (LivingEntity boss : PENDING_MOMENTUM_RESET) {
                if (boss != null && boss.isAlive()) {
                    Vec3 motion = boss.getDeltaMovement();
                    // 3% (0.03) по горизонтали, вертикальный взлет обнуляется
                    boss.setDeltaMovement(
                        motion.x * 0.03,
                        Math.min(0.0, motion.y),
                        motion.z * 0.03
                    );
                    boss.hurtMarked = true;
                }
            }
            PENDING_MOMENTUM_RESET.clear();
        }
    }

    /**
     * 4. Постоянная защита от взлета в воздух при получении урона
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        // Быстрый отсекатель обычных мобов (0% нагрузки)
        if (entity.getMaxHealth() < 80.0f && !(entity instanceof EnderDragon || entity instanceof WitherBoss)) {
            return;
        }

        Vec3 motion = entity.getDeltaMovement();

        // 4.1. 100% Блокировка улета в небо: во время анимации получения урона (hurtTime > 0)
        // подъемный вектор Y мгновенно срезается, гравитация и падение вниз работают штатно.
        if (entity.hurtTime > 0 && motion.y > 0.0) {
            entity.setDeltaMovement(motion.x * 0.03, 0.0, motion.z * 0.03);
            entity.hurtMarked = true;
            return;
        }

        // 4.2. Подавление аномального разгона по горизонтали
        double horizontalSpeedSq = motion.x * motion.x + motion.z * motion.z;
        if (entity.hurtTime > 0 && horizontalSpeedSq > 0.01) {
            entity.setDeltaMovement(motion.x * 0.03, motion.y, motion.z * 0.03);
            entity.hurtMarked = true;
        }
    }

    /**
     * Предикат боссов сборки «Аркейн»
     */
    private static boolean isBossOrHeavy(LivingEntity entity) {
        if (entity.getMaxHealth() >= 80.0f || entity instanceof EnderDragon || entity instanceof WitherBoss) {
            return true;
        }

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null) return false;
        
        String ns = id.getNamespace();
        return ns.equals("cataclysm") 
            || ns.equals("eeeabsmobs") 
            || ns.equals("snows_bosses_mechasent") 
            || ns.equals("corundumguardian") 
            || ns.equals("alexscaves") 
            || ns.equals("mowziesmobs");
    }
}