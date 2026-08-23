package com.example.arcanebridge.logic;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class AdrenalineImplantHandler {

    private static final String ADRENALINE_IMPLANT = "cyber_ware_port:lower_organs_upgrades_adrenaline";
    private static final UUID BERSERK_ATTACK_SPEED_UUID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
    private static final long COOLDOWN_MILLIS = 45_000L; // 45 секунд перезарядки

    private static final Map<UUID, Long> LAST_TRIGGER_TIME = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ACTIVE_UNTIL = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onDamageTaken(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isSpectator()) return;

        float resultingHealth = player.getHealth() - event.getAmount();
        float healthThreshold = player.getMaxHealth() * 0.30F;

        if (resultingHealth > 0 && resultingHealth <= healthThreshold) {
            if (CyberwareHelper.hasCyberware(player, ADRENALINE_IMPLANT)) {
                long now = System.currentTimeMillis();
                long lastTrigger = LAST_TRIGGER_TIME.getOrDefault(player.getUUID(), 0L);

                if (now - lastTrigger >= COOLDOWN_MILLIS) {
                    LAST_TRIGGER_TIME.put(player.getUUID(), now);
                    ACTIVE_UNTIL.put(player.getUUID(), now + 5000L); // 5 секунд действия

                    // Накладываем ускорение атаки (+40%)
                    AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
                    if (attackSpeed != null && attackSpeed.getModifier(BERSERK_ATTACK_SPEED_UUID) == null) {
                        attackSpeed.addTransientModifier(new AttributeModifier(
                                BERSERK_ATTACK_SPEED_UUID,
                                "Adrenaline Pump Berserk",
                                0.40D,
                                AttributeModifier.Operation.MULTIPLY_TOTAL
                        ));
                    }

                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, false, true));
                    player.displayClientMessage(Component.literal("§c§l[АДРЕНАЛИНОВЫЙ ВСПРЫСК]: Режим берсерка активен (5 сек)!"), true);

                    ServerLevel level = player.serverLevel();
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 1.4F);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;
        if (event.player.tickCount % 10 != 0) return;

        ServerPlayer player = (ServerPlayer) event.player;
        Long until = ACTIVE_UNTIL.get(player.getUUID());

        if (until != null && System.currentTimeMillis() >= until) {
            ACTIVE_UNTIL.remove(player.getUUID());
            AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
            if (attackSpeed != null) {
                attackSpeed.removeModifier(BERSERK_ATTACK_SPEED_UUID);
            }
        }
    }
}