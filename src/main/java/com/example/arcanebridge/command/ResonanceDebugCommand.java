package com.example.arcanebridge.command;

import com.example.arcanebridge.logic.ArmorCalibrationEngine;
import com.example.arcanebridge.logic.ElementalParasiteEngine;
import com.example.arcanebridge.logic.ResonancePenalties;
import com.example.arcanebridge.network.NetworkHandler;
import com.example.arcanebridge.network.PacketSyncControlGlitch;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class ResonanceDebugCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("arcane_debug")
                .requires(source -> source.hasPermission(2))

                // /arcane_debug penalty <mech|arcane|ele|all> <1-3>
                .then(Commands.literal("penalty")
                        .then(Commands.argument("frequency", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("mech", "arcane", "ele", "all"), builder))
                                .then(Commands.argument("tier", IntegerArgumentType.integer(1, 3))
                                        .executes(ctx -> executePenalty(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "frequency"),
                                                IntegerArgumentType.getInteger(ctx, "tier")
                                        ))
                                )
                        )
                )

                // /arcane_debug trigger <wasd_glitch|parasite_eat|parasite_break|parasite_click|jam_armor|implosion>
                .then(Commands.literal("trigger")
                        .then(Commands.argument("feature", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of(
                                        "wasd_glitch", "parasite_eat", "parasite_break", "parasite_click", "jam_armor", "implosion"
                                ), builder))
                                .executes(ctx -> executeDirectTrigger(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "feature")
                                ))
                        )
                )

                // /arcane_debug reset
                .then(Commands.literal("reset")
                        .executes(ctx -> executeReset(ctx.getSource()))
                )
        );
    }

    private static int executePenalty(CommandSourceStack source, String frequency, int tier) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        ServerLevel level = player.serverLevel();

        source.sendSuccess(() -> Component.literal("§6[ARCANE-DEBUG] §aТест пенальти: §e" + frequency + " §7(Тир " + tier + ")"), true);

        if (frequency.equals("mech") || frequency.equals("all")) {
            if (tier == 1) {
                player.swing(player.getUsedItemHand(), true);
                ArmorCalibrationEngine.jamRandomArmorPiece(player);
            } else if (tier == 2) {
                player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
                NetworkHandler.sendToPlayer(player, new PacketSyncControlGlitch(50));
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7f, 0.7f);
            } else if (tier == 3) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 15, 0.4, 0.5, 0.4, 0.1);
                player.hurt(player.damageSources().magic(), 1.0f);
            }
        }

        if (frequency.equals("arcane") || frequency.equals("all")) {
            if (tier == 1) {
                MobEffect shrinkEffect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("hexcasting", "shrink_grid"));
                if (shrinkEffect != null) player.addEffect(new MobEffectInstance(shrinkEffect, 160, 0));
            } else if (tier == 2) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9f, 1.2f);
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 1, player.getZ(), 15, 0.3, 0.3, 0.3, 0.1);
            } else if (tier == 3) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.9f, 0.7f);
                level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                player.hurt(player.damageSources().magic(), 1.0f);
            }
        }

        if (frequency.equals("ele") || frequency.equals("all")) {
            if (tier == 1) {
                ElementalParasiteEngine.triggerParasite(player, "Опять ты ломаешь баланс стихий?");
            } else if (tier == 2) {
                player.setSecondsOnFire(2);
                player.setTicksFrozen(300);
            } else if (tier == 3) {
                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
                if (lightning != null) {
                    lightning.moveTo(player.getX() + 4.0, player.getY(), player.getZ() + 4.0);
                    lightning.setVisualOnly(true);
                    level.addFreshEntity(lightning);
                }
            }
        }
        return 1;
    }

    private static int executeDirectTrigger(CommandSourceStack source, String feature) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        ServerLevel level = player.serverLevel();

        switch (feature) {
            case "wasd_glitch" -> {
                NetworkHandler.sendToPlayer(player, new PacketSyncControlGlitch(60));
                source.sendSuccess(() -> Component.literal("§a[ARCANE-DEBUG] WASD инвертирован на 3 секунды!"), true);
            }
            case "parasite_eat" -> ElementalParasiteEngine.triggerParasite(player, "А что это ты такое жуешь? Мог бы и аметист сгрызть, для проводимости.");
            case "parasite_break" -> ElementalParasiteEngine.triggerParasite(player, "Зачем ты сломал этот блок? Он тут триста лет спокойно лежал!");
            case "parasite_click" -> ElementalParasiteEngine.triggerParasite(player, "Ты точно уверен, что нажал правильную кнопку?");
            case "jam_armor" -> ArmorCalibrationEngine.jamRandomArmorPiece(player);
            case "implosion" -> {
                ResonancePenalties.triggerSafeImplosion(player, level);
                source.sendSuccess(() -> Component.literal("§a[ARCANE-DEBUG] Запущена безопасная гравитационная имплозия!"), true);
            }
        }
        return 1;
    }

    private static int executeReset(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty() && stack.hasTag()) {
                    stack.getTag().remove("Jammed");
                }
            }
        }

        MobEffect shrinkEffect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("hexcasting", "shrink_grid"));
        if (shrinkEffect != null) player.removeEffect(shrinkEffect);
        player.clearFire();
        player.setTicksFrozen(0);

        source.sendSuccess(() -> Component.literal("§a✔ [ARCANE-DEBUG] Все дебаффы, заклинивания и таймеры сброшены!"), true);
        return 1;
    }
}