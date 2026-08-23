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

public class RaidConfig {

    public static boolean enabled = true;
    public static int intervalSeconds = 1800; // 30 минут
    public static int prepDurationSeconds = 120; // 2 минуты подготовки
    public static int maxDurationSeconds = 900; // 15 минут таймаут
    public static int zoneRadiusBlocks = 50;
    public static List<String> raidTypes = new ArrayList<>();
    public static List<String> requiredAdvancements = new ArrayList<>();

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
                if (root.has("enabled")) enabled = root.get("enabled").getAsBoolean();
                if (root.has("interval_seconds")) intervalSeconds = root.get("interval_seconds").getAsInt();
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}