package com.example.arcanebridge.client;

import com.example.arcanebridge.client.gui.GuideDialogueScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ClientGuiOpener {

    private static final ResourceLocation DIALOGUE_RES = new ResourceLocation("arcane", "dialogues/guide_dialogues.json");

        public static void openGuideDialogue(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Entity entity = mc.level.getEntity(entityId);
        String jsonContent = loadDialogueJson();

        // Определение динамического состояния игрока
        String startNode = "greeting";
        boolean isInjured = mc.player.getHealth() <= 6.0F || mc.player.hasEffect(
                ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("majruszsdifficulty", "bleeding"))
        );
        boolean hasDissonance = mc.player.getPersistentData().getBoolean("ArcaneEleOverload") ||
                                mc.player.getPersistentData().getFloat("ArcaneStability") < 70.0F;

        if (isInjured) {
            startNode = "greeting_injured";
        } else if (hasDissonance) {
            startNode = "greeting_resonance_alert";
        }

                        GuideDialogueScreen screen = new GuideDialogueScreen(entity, jsonContent);
        mc.setScreen(screen);
        screen.loadNode(startNode);
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
              "npc_text": "Приветствую, странник. Эфирные потоки спокойны, готов предоставить диагностику или данные локатора.",
              "sound_event": "arcane_bridge:guide.greeting_01",
              "options": [
                { "index": 1, "text": "Открыть журнал задач (Квестбук)", "target_node": "OPEN_FTB_QUESTS" },
                { "index": 2, "text": "Что делать дальше? (Вектор прогрессии)", "target_node": "progression_hub" },
                { "index": 3, "text": "Спектральный Локатор структур", "target_node": "locator_hub" },
                { "index": 4, "text": "Проверь частоты резонанса.", "target_node": "resonance" },
                { "index": 5, "text": "[Завершить диалог]", "target_node": "EXIT" }
              ]
            },
            "greeting_injured": {
              "npc_text": "Критическая потеря био-материала! Стабилизирую твои каналы... Установи искусственные тромбоциты (Cyberware), чтобы не истекать кровью в бою.",
              "sound_event": "arcane_bridge:guide.greeting_injured",
              "options": [
                { "index": 1, "text": "Открыть журнал задач (Квестбук)", "target_node": "OPEN_FTB_QUESTS" },
                { "index": 2, "text": "Спектральный Локатор структур", "target_node": "locator_hub" },
                { "index": 3, "text": "Что делать дальше?", "target_node": "progression_hub" },
                { "index": 4, "text": "[Завершить диалог]", "target_node": "EXIT" }
              ]
            },
            "greeting_resonance_alert": {
              "npc_text": "Фиксирую частотный диссонанс! Твоя эфирная броня конфликтует. Установи Настроечную Матрицу, пока стабильность не упала.",
              "sound_event": "arcane_bridge:guide.resonance_warning",
              "options": [
                { "index": 1, "text": "Как снизить диссонанс?", "target_node": "resonance" },
                { "index": 2, "text": "Открыть журнал задач (Квестбук)", "target_node": "OPEN_FTB_QUESTS" },
                { "index": 3, "text": "[Завершить диалог]", "target_node": "EXIT" }
              ]
            },
            "progression_hub": {
              "npc_text": "Барьер Незера заблокирован. Для пробития пространственного разлома собери Эфирный Инициатор на сборочной линии конвейера.",
              "sound_event": "arcane_bridge:guide.progression_nether",
              "options": [
                { "index": 1, "text": "[Назад]", "target_node": "greeting" },
                { "index": 2, "text": "[Завершить диалог]", "target_node": "EXIT" }
              ]
            },
            "locator_hub": {
              "npc_text": "Укажи целевой энергетический след для калибровки спектрального сенсора:",
              "sound_event": "arcane_bridge:guide.locator_menu",
              "options": [
                { "index": 1, "text": "Подземелье Кователя (Wroughtnaut Chamber)", "target_node": "ACTION_LOCATE:mowziesmobs:wroughtnaut_chamber" },
                { "index": 2, "text": "Древняя Фабрика (Ancient Factory / Harbinger)", "target_node": "ACTION_LOCATE:cataclysm:ancient_factory" },
                { "index": 3, "text": "Пылающая Арена (Burning Arena / Ignis)", "target_node": "ACTION_LOCATE:cataclysm:burning_arena" },
                { "index": 4, "text": "[Назад]", "target_node": "greeting" }
              ]
            },
            "resonance": {
              "npc_text": "Следи за нагрузкой частот. Ношение более 2 предметов одной категории (Mech / Arcane / Ele) без Матрицы снижает стабильность тела.",
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