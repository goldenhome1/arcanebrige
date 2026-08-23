package com.example.arcanebridge.raid;

import com.example.arcanebridge.network.ClientboundRaidVfxPacket;
import com.example.arcanebridge.network.NetworkHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.advancements.Advancement;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class RaidCoordinator {

    private static boolean isRaidActive = false;
    private static int elapsedRaidSeconds = 0;
    private static BlockPos raidEpicenter = BlockPos.ZERO;

    private static boolean isPrepActive = false;
    private static int prepTimerSeconds = 0;
    private static int maxPrepSeconds = 120;
    private static UUID targetPlayerUUID = null;
    private static String currentRaidType = "arcane_breach";

    // Режим тестирования визуала (без мобов)
    private static boolean isVfxTestOnly = false;
    private static int vfxRaidDisplaySeconds = 0;

    // Сохраненное состояние мира до теста
    private static long savedDayTime = -1L;
    private static boolean savedRaining = false;
    private static boolean savedThundering = false;
    private static ResourceKey<Level> savedDimension = null;

    private static int globalCooldownTimer = 0;
    private static final Set<UUID> playersInZone = new HashSet<>();

    public static boolean isPlayerInRaidZone(ServerPlayer player) {
        return isRaidActive && playersInZone.contains(player.getUUID());
    }

    public static void startVfxTest(ServerPlayer player, String raidType, int prepSeconds) {
        if (player == null || player.getServer() == null) return;

        ServerLevel level = player.serverLevel();
        savedDayTime = level.getDayTime();
        savedRaining = level.isRaining();
        savedThundering = level.isThundering();
        savedDimension = level.dimension();

        isVfxTestOnly = true;
        vfxRaidDisplaySeconds = 15;
        startPreparation(player, raidType, prepSeconds);
        player.sendSystemMessage(Component.literal("§d[VFX-ТЕСТ] Запущен визуальный симулятор аномалии на " + prepSeconds + " сек."));
    }

    public static void startPreparation(ServerPlayer player, String raidType, int customPrepSeconds) {
        if (player == null || player.getServer() == null) return;

        // 🛡️ Защита: не запускаем рейд на мирной сложности
        if (!isVfxTestOnly && player.serverLevel().getDifficulty() == Difficulty.PEACEFUL) {
            player.sendSystemMessage(Component.literal("§c[СИСТЕМА] Рейды заблокированы: на сервере установлена Мирная сложность (Peaceful)!"));
            return;
        }

        MinecraftServer server = player.getServer();

        isPrepActive = true;
        maxPrepSeconds = customPrepSeconds > 0 ? customPrepSeconds : RaidConfig.prepDurationSeconds;
        prepTimerSeconds = maxPrepSeconds;
        targetPlayerUUID = player.getUUID();
        currentRaidType = (raidType != null && !raidType.isEmpty()) ? raidType : getRandomRaidType();

        String timeText = prepTimerSeconds >= 60 ? (prepTimerSeconds / 60) + " мин." : prepTimerSeconds + " сек.";

        if (!isVfxTestOnly) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§l§6[ВНИМАНИЕ] §eПространственная аномалия сформируется в районе игрока §c"
                            + player.getGameProfile().getName() + " §eчерез §c" + timeText + "§e! (§c" + currentRaidType + "§e)"),
                    false
            );
        }

        syncVfxToAll();

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.playNotifySound(SoundEvents.PORTAL_TRIGGER, SoundSource.AMBIENT, 0.8F, 0.6F);
        }
    }

    public static void startRaid(MinecraftServer server, ServerPlayer player, String raidType) {
        if (server == null || player == null) return;

        // 🛡️ Повторная проверка сложности при наступлении 0 секунд
        if (!isVfxTestOnly && player.serverLevel().getDifficulty() == Difficulty.PEACEFUL) {
            stopRaid(server);
            player.sendSystemMessage(Component.literal("§c[СИСТЕМА] Рейд отменен из-за мирной сложности мира."));
            return;
        }

        isPrepActive = false;
        prepTimerSeconds = 0;
        isRaidActive = true;
        elapsedRaidSeconds = 0;
        targetPlayerUUID = player.getUUID();
        raidEpicenter = player.blockPosition();
        currentRaidType = raidType != null ? raidType : currentRaidType;

        ServerLevel level = player.serverLevel();
        level.setDayTime(18000); // Ночь
        level.setWeatherParameters(0, 6000, true, true); // Гроза

        player.playNotifySound(SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.MASTER, 1.0F, 0.8F);

        syncVfxToAll();

        if (isVfxTestOnly) {
            player.sendSystemMessage(Component.literal("§d[VFX-ТЕСТ] 💥 МОМЕНТ ПРОРЫВА! Разлом распахнут в небе."));
            return;
        }

        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§l§c[ПРОРЫВ] §cАномалия разверзлась! Рейд §e" + currentRaidType + " §cначался вокруг §e"
                        + player.getGameProfile().getName() + "§c!"),
                false
        );

        CommandSourceStack elevatedSource = player.createCommandSourceStack().withPermission(4);
        server.getCommands().performPrefixedCommand(elevatedSource, "ravents raids " + currentRaidType);
    }

    public static void stopRaid(MinecraftServer server) {
        if (server != null) {
            // 🛑 Гарантированно гасим волны мода Ravents
            try {
                server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack().withPermission(4),
                        "ravents stop"
                );
            } catch (Exception ignored) {}
        }

        // Восстановление погоды и времени после VFX-теста
        if (server != null && savedDimension != null && savedDayTime != -1L) {
            ServerLevel level = server.getLevel(savedDimension);
            if (level != null) {
                level.setDayTime(savedDayTime);
                if (!savedRaining && !savedThundering) {
                    level.setWeatherParameters(6000, 0, false, false);
                } else {
                    level.setWeatherParameters(0, 6000, savedRaining, savedThundering);
                }
            }
        }

        savedDayTime = -1L;
        savedDimension = null;
        isVfxTestOnly = false;
        vfxRaidDisplaySeconds = 0;

        isRaidActive = false;
        elapsedRaidSeconds = 0;
        raidEpicenter = BlockPos.ZERO;

        isPrepActive = false;
        prepTimerSeconds = 0;
        targetPlayerUUID = null;
        playersInZone.clear();

        syncVfxToAll();

        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§l§2[СИСТЕМА] §aПространственная аномалия рассеялась. Угроза нейтрализована."),
                    false
            );
        }
    }

    private static void syncVfxToAll() {
        NetworkHandler.sendToAll(new ClientboundRaidVfxPacket(
                isPrepActive,
                prepTimerSeconds,
                maxPrepSeconds,
                isRaidActive,
                currentRaidType,
                targetPlayerUUID,
                raidEpicenter
        ));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null || server.getTickCount() % 20 != 0) return;

        // 1. АКТИВНЫЙ РЕЙД ИЛИ VFX ТЕСТ
        if (isRaidActive) {
            elapsedRaidSeconds++;

            if (isVfxTestOnly) {
                if (elapsedRaidSeconds >= vfxRaidDisplaySeconds) {
                    stopRaid(server);
                    return;
                }
            } else if (elapsedRaidSeconds >= RaidConfig.maxDurationSeconds) {
                stopRaid(server);
                return;
            }

            playersInZone.clear();
            double radiusSq = RaidConfig.zoneRadiusBlocks * RaidConfig.zoneRadiusBlocks;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.blockPosition().distSqr(raidEpicenter) <= radiusSq) {
                    playersInZone.add(player.getUUID());
                }
            }

            syncVfxToAll();
            return;
        }

        // 2. ФАЗА ПОДГОТОВКИ
        if (isPrepActive) {
            prepTimerSeconds--;
            ServerPlayer target = targetPlayerUUID != null ? server.getPlayerList().getPlayer(targetPlayerUUID) : null;

            if (target == null) {
                stopRaid(server);
                return;
            }

            target.displayClientMessage(Component.literal("§e⚡ " + (isVfxTestOnly ? "[VFX ТЕСТ] " : "") + "Подготовка: §c" + prepTimerSeconds + " сек."), true);

            if (prepTimerSeconds <= 60 && prepTimerSeconds > 0) {
                ServerLevel sl = target.serverLevel();
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.5, 0.5, 0.5, 0.05);

                if (prepTimerSeconds % 10 == 0) {
                    target.playNotifySound(SoundEvents.BEACON_AMBIENT, SoundSource.AMBIENT, 1.0F, 1.5F);
                }
            }

            syncVfxToAll();

            if (prepTimerSeconds <= 0) {
                startRaid(server, target, currentRaidType);
            }
            return;
        }

        // 3. АВТО-ТАЙМЕР (блокируется, если выключен или стоит мирная сложность)
        if (!RaidConfig.enabled) return;
        if (server.overworld().getDifficulty() == Difficulty.PEACEFUL) return;

        globalCooldownTimer++;
        if (globalCooldownTimer >= RaidConfig.intervalSeconds) {
            globalCooldownTimer = 0;

            List<ServerPlayer> validCandidates = new ArrayList<>();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.isCreative() || player.isSpectator()) continue;
                if (hasRequiredAdvancements(player, server)) {
                    validCandidates.add(player);
                }
            }

            if (!validCandidates.isEmpty()) {
                ServerPlayer selected = validCandidates.get(new Random().nextInt(validCandidates.size()));
                startPreparation(selected, getRandomRaidType(), RaidConfig.prepDurationSeconds);
            }
        }
    }

    private static boolean hasRequiredAdvancements(ServerPlayer player, MinecraftServer server) {
        if (RaidConfig.requiredAdvancements.isEmpty()) return true;
        for (String advId : RaidConfig.requiredAdvancements) {
            Advancement adv = server.getAdvancements().getAdvancement(new ResourceLocation(advId));
            if (adv != null && player.getAdvancements().getOrStartProgress(adv).isDone()) {
                return true;
            }
        }
        return false;
    }

    private static String getRandomRaidType() {
        if (RaidConfig.raidTypes.isEmpty()) return "arcane_breach";
        return RaidConfig.raidTypes.get(new Random().nextInt(RaidConfig.raidTypes.size()));
    }

    private static ServerPlayer resolveTargetPlayer(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player != null) return player;

        List<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
        if (!players.isEmpty()) {
            return players.get(0);
        }
        return null;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("arcane_test_vfx")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    ServerPlayer target = resolveTargetPlayer(ctx.getSource());
                    if (target == null) return 0;
                    startVfxTest(target, "arcane_breach", 10);
                    return 1;
                })
                .then(Commands.argument("seconds", IntegerArgumentType.integer(3, 300))
                        .executes(ctx -> {
                            ServerPlayer target = resolveTargetPlayer(ctx.getSource());
                            if (target == null) return 0;
                            int sec = IntegerArgumentType.getInteger(ctx, "seconds");
                            startVfxTest(target, "arcane_breach", sec);
                            return 1;
                        })
                        .then(Commands.argument("type", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer target = resolveTargetPlayer(ctx.getSource());
                                    if (target == null) return 0;
                                    int sec = IntegerArgumentType.getInteger(ctx, "seconds");
                                    String type = StringArgumentType.getString(ctx, "type");
                                    startVfxTest(target, type, sec);
                                    return 1;
                                })))
        );

        dispatcher.register(Commands.literal("arcane_test_prep_quick")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    ServerPlayer target = resolveTargetPlayer(ctx.getSource());
                    if (target == null) return 0;
                    startPreparation(target, getRandomRaidType(), 10);
                    return 1;
                })
        );

        dispatcher.register(Commands.literal("arcane_test_prep_raid")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    ServerPlayer target = resolveTargetPlayer(ctx.getSource());
                    if (target == null) return 0;
                    startPreparation(target, getRandomRaidType(), RaidConfig.prepDurationSeconds);
                    return 1;
                })
        );

        dispatcher.register(Commands.literal("arcane_test_raid")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    ServerPlayer target = resolveTargetPlayer(ctx.getSource());
                    if (target == null) return 0;
                    startRaid(target.getServer(), target, getRandomRaidType());
                    return 1;
                })
        );

        dispatcher.register(Commands.literal("arcane_reset_raid")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    stopRaid(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal("§a[СИСТЕМА] Рейд и таймеры сброшены!"), true);
                    return 1;
                })
        );

        dispatcher.register(Commands.literal("arcane_internal_reset_raid")
                .executes(ctx -> {
                    stopRaid(ctx.getSource().getServer());
                    return 1;
                })
        );
    }
}