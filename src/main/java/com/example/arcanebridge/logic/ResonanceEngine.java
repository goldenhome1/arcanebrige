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
import java.util.Map;

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
            inventory.findCurios("resonance_matrix").forEach(slotResult -> {
                ItemStack stack = slotResult.stack();
                if (!stack.isEmpty()) {
                    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
                    if (rl != null) {
                        String id = rl.toString().toLowerCase();
                        dynamicLimits[0] += MATRIX_MECH_BONUS.getOrDefault(id, 0);
                        dynamicLimits[1] += MATRIX_ARCANE_BONUS.getOrDefault(id, 0);
                        dynamicLimits[2] += MATRIX_ELE_BONUS.getOrDefault(id, 0);
                    }
                }
            });
        });

        Map<String, Integer> currentCounts = new HashMap<>();
        currentCounts.put("mechanical", 0);
        currentCounts.put("arcane", 0);
        currentCounts.put("elemental", 0);

        for (ItemStack armorStack : player.getArmorSlots()) {
            if (!armorStack.isEmpty()) {
                String freq = getEquipmentFrequency(armorStack);
                if (freq != null && currentCounts.containsKey(freq)) {
                    currentCounts.put(freq, currentCounts.get(freq) + 1);
                }
            }
        }

        boolean mechOverloaded = currentCounts.get("mechanical") > dynamicLimits[0];
        boolean arcaneOverloaded = currentCounts.get("arcane") > dynamicLimits[1];
        boolean eleOverloaded = currentCounts.get("elemental") > dynamicLimits[2];

        int totalOverload = 0;
        if (mechOverloaded) totalOverload += (currentCounts.get("mechanical") - dynamicLimits[0]);
        if (arcaneOverloaded) totalOverload += (currentCounts.get("arcane") - dynamicLimits[1]);
        if (eleOverloaded) totalOverload += (currentCounts.get("elemental") - dynamicLimits[2]);

        final int finalOverload = totalOverload;
        final float[] currentStability = new float[]{100.0f};

        player.getCapability(ResonanceProvider.RESONANCE).ifPresent(res -> {
            if (finalOverload > 0) {
                res.addStability(-finalOverload * 2.5f);
            } else {
                res.addStability(1.0f);
            }
            currentStability[0] = res.getStability();
            ResonancePenalties.apply(player, currentStability[0], mechOverloaded, arcaneOverloaded, eleOverloaded);
        });

        CompoundTag pData = player.getPersistentData();
        pData.putBoolean("ArcaneMechOverload", mechOverloaded);
        pData.putBoolean("ArcaneArcaneOverload", arcaneOverloaded);
        pData.putBoolean("ArcaneEleOverload", eleOverloaded);
        pData.putFloat("ArcaneStability", currentStability[0]);

        int mechTimer = Math.max(0, pData.getInt("mech_shock_timer") - 10);
        int arcaneTimer = Math.max(0, pData.getInt("arcane_shock_timer") - 10);
        int eleTimer = Math.max(0, pData.getInt("ele_shock_timer") - 10);
        pData.putInt("mech_shock_timer", mechTimer);
        pData.putInt("arcane_shock_timer", arcaneTimer);
        pData.putInt("ele_shock_timer", eleTimer);

        boolean inRaidZone = RaidCoordinator.isPlayerInRaidZone(serverPlayer);

        NetworkHandler.sendToPlayer(
                new ClientboundResonanceSyncPacket(
                        currentStability[0],
                        currentCounts.get("mechanical"),
                        currentCounts.get("arcane"),
                        currentCounts.get("elemental"),
                        dynamicLimits[0], dynamicLimits[1], dynamicLimits[2],
                        mechTimer > 0, arcaneTimer > 0, eleTimer > 0,
                        inRaidZone
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
}