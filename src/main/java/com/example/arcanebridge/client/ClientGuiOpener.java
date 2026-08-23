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
              "npc_text": "Приветствую, странник. Эфирные потоки спокойны, готов ответить на твои вопросы.",
              "sound_event": "arcane_bridge:guide.greeting_01",
              "options": [
                { "index": 1, "text": "Что делать дальше?", "target_node": "progression" },
                { "index": 2, "text": "Проверь частоты резонанса.", "target_node": "resonance" },
                { "index": 3, "text": "[Завершить диалог]", "target_node": "EXIT" }
              ]
            },
            "progression": {
              "npc_text": "Для стабилизации разлома и выхода в Незер тебе понадобится Эфирный Инициатор. Собери его на конвейере Create.",
              "sound_event": "arcane_bridge:guide.progression_nether",
              "options": [
                { "index": 1, "text": "[Назад]", "target_node": "greeting" },
                { "index": 2, "text": "[Завершить диалог]", "target_node": "EXIT" }
              ]
            },
            "resonance": {
              "npc_text": "Следи за нагрузкой частот. Ношение более 2 предметов одной категории без Матрицы вызовет диссонанс.",
              "sound_event": "arcane_bridge:guide.resonance_warning",
              "options": [
                { "index": 1, "text": "[Назад]", "target_node": "greeting" },
                { "index": 2, "text": "[Завершить диалог]", "target_node": "EXIT" }
              ]
            }
          }
        }
        """;
    }
}