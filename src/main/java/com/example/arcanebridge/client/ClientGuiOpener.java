package com.example.arcanebridge.client;

import com.example.arcanebridge.client.gui.GuideDialogueScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ClientGuiOpener {

    private static final ResourceLocation DIALOGUE_RES = new ResourceLocation("arcane", "dialogues/guide_dialogues.json");

    public static void openGuideDialogue(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getEntity(entityId);
        String jsonContent = loadDialogueJson();

        mc.setScreen(new GuideDialogueScreen(entity, jsonContent));
    }

    private static String loadDialogueJson() {
        try {
            var resourceManager = Minecraft.getInstance().getResourceManager();
            var resourceOpt = resourceManager.getResource(DIALOGUE_RES);
            if (resourceOpt.isPresent()) {
                Resource res = resourceOpt.get();
                try (var reader = new InputStreamReader(res.open(), StandardCharsets.UTF_8)) {
                    StringBuilder sb = new StringBuilder();
                    char[] buf = new char[1024];
                    int read;
                    while ((read = reader.read(buf)) != -1) {
                        sb.append(buf, 0, read);
                    }
                    return sb.toString();
                }
            }
        } catch (Exception ignored) {}

        // Резервный JSON, если файл не найден в ресурсах
        return """
        {
          "start_node": "greeting",
          "nodes": {
            "greeting": {
              "npc_text": "Приветствую, пилот. Эфирные потоки стабильны, модули в боевой готовности.",
              "sound_event": "arcane_bridge:guide.greeting_01",
              "options": [
                { "index": 1, "text": "Что делать дальше?", "target_node": "EXIT" },
                { "index": 2, "text": "[Завершить диалог]", "target_node": "EXIT" }
              ]
            }
          }
        }
        """;
    }
}