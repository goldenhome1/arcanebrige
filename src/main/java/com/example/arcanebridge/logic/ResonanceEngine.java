package com.example.arcanebridge.logic;

import com.example.arcanebridge.capability.ResonanceProvider;
import com.example.arcanebridge.network.ClientboundResonanceSyncPacket;
import com.example.arcanebridge.network.NetworkHandler;
import com.example.arcanebridge.raid.RaidCoordinator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = "arcane_bridge")
public class ResonanceEngine {

    private static final Map<String, String> CUSTOM_FREQUENCIES = new HashMap<>();
    private static final Map<String, Integer> MATRIX_MECH_BONUS = new HashMap<>();
    private static final Map<String, Integer> MATRIX_ARCANE_BONUS = new HashMap<>();
    private static final Map<String, Integer> MATRIX_ELE_BONUS = new HashMap<>();

    public static void loadOrCreateConfig() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("arcane_bridge_resonance.json");
        File configFile = configPath.toFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (!configFile.exists()) {
            try {
                configFile.getParentFile().mkdirs();
                JsonObject root = new JsonObject();

                JsonObject frequencies = new JsonObject();
                frequencies.addProperty("ars_nouveau:sorcerer_hood", "arcane");
                frequencies.addProperty("ars_elemental:fire_boots", "elemental");
                frequencies.addProperty("create:diving_helmet", "mechanical");
                root.add("frequencies", frequencies);

                JsonObject matrices = new JsonObject();
                JsonObject basicMatrix = new JsonObject();
                basicMatrix.addProperty("mechanical", 1);
                basicMatrix.addProperty("arcane", 1);
                basicMatrix.addProperty("elemental", 1);
                matrices.add("kubejs:basic_universal_matrix", basicMatrix);
                root.add("matrices", matrices);

                try (FileWriter writer = new FileWriter(configFile)) {
                    gson.toJson(root, writer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (FileReader reader = new FileReader(configFile)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root != null) {
                if (root.has("frequencies")) {
                    CUSTOM_FREQUENCIES.clear();
                    JsonObject frequencies = root.getAsJsonObject("frequencies");
                    for (Map.Entry<String, JsonElement> entry : frequencies.entrySet()) {
                        CUSTOM_FREQUENCIES.put(entry.getKey().toLowerCase(), entry.getValue().getAsString().toLowerCase());
                    }
                }
                if (root.has("matrices")) {
                    MATRIX_MECH_BONUS.clear();
                    MATRIX_ARCANE_BONUS.clear();
                    MATRIX_ELE_BONUS.clear();
                    JsonObject matrices = root.getAsJsonObject("matrices");
                    for (Map.Entry<String, JsonElement> entry : matrices.entrySet()) {
                        String matrixId = entry.getKey().toLowerCase();
                        if (entry.getValue().isJsonObject()) {
                            JsonObject stats = entry.getValue().getAsJsonObject();
                            if (stats.has("mechanical")) MATRIX_MECH_BONUS.put(matrixId, stats.get("mechanical").getAsInt());
                            if (stats.has("arcane")) MATRIX_ARCANE_BONUS.put(matrixId, stats.get("arcane").getAsInt());
                            if (stats.has("elemental")) MATRIX_ELE_BONUS.put(matrixId, stats.get("elemental").getAsInt());
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;

        Player player = event.player;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        if (player.tickCount % 10 != 0) return;

        final int[] dynamicLimits = new int[]{2, 2, 2};

        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
            Set<String> equippedMatrixIds = new HashSet<>();
            inventory.findCurios("resonance_matrix").forEach(slotResult -> {
                ItemStack stack = slotResult.stack();
                if (!stack.isEmpty()) {
                    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
                    if (rl != null) {
                        String id = rl.toString().toLowerCase();
                        // Запрет дубликатов: одинаковые матрицы глушат друг друга (деструктивная интерференция)
                        if (equippedMatrixIds.add(id)) {
                            dynamicLimits[0] += MATRIX_MECH_BONUS.getOrDefault(id, 0);
                            dynamicLimits[1] += MATRIX_ARCANE_BONUS.getOrDefault(id, 0);
                            dynamicLimits[2] += MATRIX_ELE_BONUS.getOrDefault(id, 0);
                        }
                    }
                }
            });

            // Эндгейм-синергии при сочетании специализированных и универсальных матриц
            boolean hasFocusedArcane = equippedMatrixIds.contains("kubejs:focused_arcane_matrix");
            boolean hasFocusedMech = equippedMatrixIds.contains("kubejs:focused_mechanical_matrix");
            boolean hasAdvUniversal = equippedMatrixIds.contains("kubejs:advanced_universal_matrix");
            boolean hasCosmic = equippedMatrixIds.contains("kubejs:cosmic_universal_matrix");

            boolean tendrilMastery = (hasFocusedArcane && (hasAdvUniversal || hasCosmic)) || hasCosmic;
            boolean overclockMastery = (hasFocusedMech && (hasAdvUniversal || hasCosmic)) || hasCosmic;
            boolean cosmicSynergy = hasCosmic || (hasFocusedMech && hasFocusedArcane);

            CompoundTag pData = player.getPersistentData();
            pData.putBoolean("MatrixTendrilMastery", tendrilMastery);
            pData.putBoolean("MatrixOverclockMastery", overclockMastery);
            pData.putBoolean("MatrixCosmicSynergy", cosmicSynergy);
        });

        CompoundTag pData = player.getPersistentData();

        // 1. Декремент таймеров шока (проверка происходит раз в 10 тиков)
        int mechTimer = pData.getInt("mech_shock_timer");
        int arcaneTimer = pData.getInt("arcane_shock_timer");
        int eleTimer = pData.getInt("ele_shock_timer");

        float mechBonus = mechTimer > 0 ? (pData.contains("mech_caster_load", 5) ? pData.getFloat("mech_caster_load") : (float) pData.getInt("mech_caster_load")) : 0.0f;
        float arcaneBonus = arcaneTimer > 0 ? (pData.contains("arcane_caster_load", 5) ? pData.getFloat("arcane_caster_load") : (float) pData.getInt("arcane_caster_load")) : 0.0f;
        float eleBonus = eleTimer > 0 ? (pData.contains("ele_caster_load", 5) ? pData.getFloat("ele_caster_load") : (float) pData.getInt("ele_caster_load")) : 0.0f;

        mechTimer = Math.max(0, mechTimer - 10);
        arcaneTimer = Math.max(0, arcaneTimer - 10);
        eleTimer = Math.max(0, eleTimer - 10);
        pData.putInt("mech_shock_timer", mechTimer);
        pData.putInt("arcane_shock_timer", arcaneTimer);
        pData.putInt("ele_shock_timer", eleTimer);

        if (mechTimer <= 0) pData.remove("mech_caster_load");
        if (arcaneTimer <= 0) pData.remove("arcane_caster_load");
        if (eleTimer <= 0) pData.remove("ele_caster_load");

        // 2. Расчет общей нагрузки: надетая физическая броня + эфирная броня Curios + виртуальная нагрузка оружия
        Map<String, Float> currentCounts = new HashMap<>();
        currentCounts.put("mechanical", mechBonus);
        currentCounts.put("arcane", arcaneBonus);
        currentCounts.put("elemental", eleBonus);

        // Сканирование строго 4 ванильных физических слотов инвентаря
        for (ItemStack armorStack : player.getInventory().armor) {
            if (!armorStack.isEmpty()) {
                String freq = getEquipmentFrequency(armorStack);
                if (freq != null && currentCounts.containsKey(freq)) {
                    int weight = getSlotWeight(armorStack);
                    currentCounts.put(freq, currentCounts.get(freq) + (float) weight);
                }
            }
        }

        // Сканирование эфирных слотов брони в Curios (до 8 слотов всего)
        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
            for (String etherSlot : java.util.List.of("arcane_helmet", "arcane_chestplate", "arcane_leggings", "arcane_boots")) {
                inventory.findCurios(etherSlot).forEach(slotResult -> {
                    ItemStack stack = slotResult.stack();
                    if (!stack.isEmpty()) {
                        String freq = getEquipmentFrequency(stack);
                        if (freq != null && currentCounts.containsKey(freq)) {
                            int weight = etherSlot.equals("arcane_chestplate") ? 2 : getSlotWeight(stack);
                            currentCounts.put(freq, currentCounts.get(freq) + (float) weight);
                        }
                    }
                });
            }
        });

        boolean mechOverloaded = currentCounts.get("mechanical") > (float) dynamicLimits[0];
        boolean arcaneOverloaded = currentCounts.get("arcane") > (float) dynamicLimits[1];
        boolean eleOverloaded = currentCounts.get("elemental") > (float) dynamicLimits[2];

        float totalOverload = 0.0f;
        if (mechOverloaded) totalOverload += (currentCounts.get("mechanical") - (float) dynamicLimits[0]);
        if (arcaneOverloaded) totalOverload += (currentCounts.get("arcane") - (float) dynamicLimits[1]);
        if (eleOverloaded) totalOverload += (currentCounts.get("elemental") - (float) dynamicLimits[2]);

        final float finalOverload = totalOverload;
        final float[] currentStability = new float[]{100.0f};

        player.getCapability(ResonanceProvider.RESONANCE).ifPresent(res -> {
            float curStab = res.getStability();
            // Шкала тиров (18% за 1 ед нагрузки):
            float targetStability = Math.max(0.0f, 100.0f - (finalOverload * 18.0f));
            float diff = targetStability - curStab;

            if (diff < 0) {
                // При перегрузе стабильность падает
                res.addStability(Math.max(diff, -2.5f * Math.max(1.0f, finalOverload)));
            } else if (diff > 0) {
                // После затухания нагрузки стабильность плавно регенерирует
                res.addStability(Math.min(diff, 1.25f));
            }

            currentStability[0] = res.getStability();
            ResonancePenalties.apply(player, currentStability[0], mechOverloaded, arcaneOverloaded, eleOverloaded);
        });

        pData.putBoolean("ArcaneMechOverload", mechOverloaded);
        pData.putBoolean("ArcaneArcaneOverload", arcaneOverloaded);
        pData.putBoolean("ArcaneEleOverload", eleOverloaded);
        pData.putFloat("ArcaneStability", currentStability[0]);

        boolean inRaidZone = RaidCoordinator.isPlayerInRaidZone(serverPlayer);

        int tStacks = pData.getInt("tendril_stacks");
        int tAwake = pData.getInt("tendril_awakening_ticks");
        int mCharge = pData.getInt("mech_charge_ticks");
        int mBars = Math.min(4, mCharge / 25);

        long currentTick = serverPlayer.level().getGameTime();
        long tCdUntil = pData.getLong("tendril_cooldown_until");
        int tCd = (tAwake <= 0 && currentTick < tCdUntil) ? (int) (tCdUntil - currentTick) : 0;
        int tMaxCd = Math.max(1, pData.getInt("tendril_max_cooldown"));

        long mCdUntil = pData.getLong("mech_overclock_cooldown");
        boolean hasOverclock = serverPlayer.hasEffect(com.example.arcanebridge.registry.ModEffects.KINETIC_OVERCLOCK.get());
        int mCd = (!hasOverclock && currentTick < mCdUntil) ? (int) (mCdUntil - currentTick) : 0;
        int mMaxCd = Math.max(1, pData.getInt("mech_max_cooldown"));

        NetworkHandler.sendToPlayer(
                new ClientboundResonanceSyncPacket(
                        currentStability[0],
                        Math.round(currentCounts.get("mechanical")),
                        Math.round(currentCounts.get("arcane")),
                        Math.round(currentCounts.get("elemental")),
                        dynamicLimits[0], dynamicLimits[1], dynamicLimits[2],
                        mechTimer > 0, arcaneTimer > 0, eleTimer > 0,
                        inRaidZone,
                        tStacks, tAwake, mBars,
                        tCd, tMaxCd, mCd, mMaxCd
                ),
                serverPlayer
        );
    }

    public static String getEquipmentFrequency(ItemStack stack) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null) return null;
        String itemId = rl.toString().toLowerCase();

        if (CUSTOM_FREQUENCIES.containsKey(itemId)) return CUSTOM_FREQUENCIES.get(itemId);
        if (itemId.contains("fire") || itemId.contains("pyro") || itemId.contains("air") || itemId.contains("aero") ||
                itemId.contains("earth") || itemId.contains("geo") || itemId.contains("water") || itemId.contains("aqua")) return "elemental";
        if (itemId.contains("ars_nouveau") || itemId.contains("sorcerer") || itemId.contains("arcanist") || itemId.contains("battlemage")) return "arcane";
        if (itemId.contains("create") || itemId.contains("cardboard") || itemId.contains("cyberware") || itemId.contains("cyber")) return "mechanical";

        return null;
    }

    public static int getSlotWeight(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        EquipmentSlot slot = LivingEntity.getEquipmentSlotForItem(stack);
        if (slot == EquipmentSlot.CHEST) return 2;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl != null) {
            String path = rl.getPath().toLowerCase();
            if (path.contains("chest") || path.contains("robe") || path.contains("tunic") || path.contains("cuirass") || path.contains("breastplate")) {
                return 2;
            }
        }
        return 1;
    }
}