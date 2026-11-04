package com.example.arcanebridge.raid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RaidConfig {

    public static boolean enabled = true;
    public static int intervalSeconds = 1800; // 30 минут по умолчанию
    public static int minIntervalSeconds = 1800;
    public static int maxIntervalSeconds = 1800;
    public static int prepDurationSeconds = 120; // 2 минуты подготовки
    public static int maxDurationSeconds = 900; // 15 минут таймаут
    public static int zoneRadiusBlocks = 50;
    public static List<String> raidTypes = new ArrayList<>();
    public static List<String> requiredAdvancements = new ArrayList<>();

    public static int getNextIntervalSeconds() {
        if (maxIntervalSeconds <= minIntervalSeconds) {
            return Math.max(10, minIntervalSeconds);
        }
        return minIntervalSeconds + new Random().nextInt(maxIntervalSeconds - minIntervalSeconds + 1);
    }

    public static void loadOrCreateConfig() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("arcane_bridge_raids.json");
        File configFile = configPath.toFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (!configFile.exists()) {
            try {
                configFile.getParentFile().mkdirs();
                JsonObject root = new JsonObject();
                root.addProperty("enabled", true);
                root.addProperty("interval_seconds", 1800);
                root.addProperty("min_interval_seconds", 1800);
                root.addProperty("max_interval_seconds", 1800);
                root.addProperty("prep_duration_seconds", 120);
                root.addProperty("max_duration_seconds", 900);
                root.addProperty("zone_radius_blocks", 50);

                JsonArray types = new JsonArray();
                types.add("arcane_breach");
                types.add("syndicate_raid");
                types.add("thermal_surge");
                root.add("raid_types", types);

                JsonArray advs = new JsonArray();
                advs.add("minecraft:nether/root");
                advs.add("minecraft:end/root");
                root.add("required_advancements", advs);

                try (FileWriter writer = new FileWriter(configFile)) {
                    gson.toJson(root, writer);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try (FileReader reader = new FileReader(configFile)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root != null) {
                boolean needsRewrite = false;

                if (root.has("enabled")) enabled = root.get("enabled").getAsBoolean();
                if (root.has("interval_seconds")) {
                    intervalSeconds = root.get("interval_seconds").getAsInt();
                    minIntervalSeconds = intervalSeconds;
                    maxIntervalSeconds = intervalSeconds;
                }
                if (root.has("min_interval_seconds")) {
                    minIntervalSeconds = root.get("min_interval_seconds").getAsInt();
                } else {
                    needsRewrite = true;
                }
                if (root.has("max_interval_seconds")) {
                    maxIntervalSeconds = root.get("max_interval_seconds").getAsInt();
                } else {
                    needsRewrite = true;
                }
                if (maxIntervalSeconds < minIntervalSeconds) {
                    maxIntervalSeconds = minIntervalSeconds;
                }
                if (root.has("prep_duration_seconds")) prepDurationSeconds = root.get("prep_duration_seconds").getAsInt();
                if (root.has("max_duration_seconds")) maxDurationSeconds = root.get("max_duration_seconds").getAsInt();
                if (root.has("zone_radius_blocks")) zoneRadiusBlocks = root.get("zone_radius_blocks").getAsInt();

                raidTypes.clear();
                if (root.has("raid_types")) {
                    root.getAsJsonArray("raid_types").forEach(el -> raidTypes.add(el.getAsString()));
                }
                if (raidTypes.isEmpty()) {
                    raidTypes.add("arcane_breach");
                }

                requiredAdvancements.clear();
                if (root.has("required_advancements")) {
                    root.getAsJsonArray("required_advancements").forEach(el -> requiredAdvancements.add(el.getAsString()));
                }

                if (needsRewrite) {
                    try {
                        JsonObject updated = new JsonObject();
                        updated.addProperty("enabled", enabled);
                        updated.addProperty("interval_seconds", intervalSeconds);
                        updated.addProperty("min_interval_seconds", minIntervalSeconds);
                        updated.addProperty("max_interval_seconds", maxIntervalSeconds);
                        updated.addProperty("prep_duration_seconds", prepDurationSeconds);
                        updated.addProperty("max_duration_seconds", maxDurationSeconds);
                        updated.addProperty("zone_radius_blocks", zoneRadiusBlocks);

                        JsonArray types = new JsonArray();
                        raidTypes.forEach(types::add);
                        updated.add("raid_types", types);

                        JsonArray advs = new JsonArray();
                        requiredAdvancements.forEach(advs::add);
                        updated.add("required_advancements", advs);

                        try (FileWriter writer = new FileWriter(configFile)) {
                            gson.toJson(updated, writer);
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}