{
  "start_node": "greeting",
  "nodes": {
    "greeting": {
      "npc_text": "Приветствую, странник. Эфирные потоки спокойны, готов ответить на твои вопросы.",
      "sound_event": "arcane_bridge:guide.greeting_01",
      "options": [
        { "index": 1, "text": "Открыть журнал задач (Квестбук)", "target_node": "OPEN_FTB_QUESTS" },
        { "index": 2, "text": "Что делать дальше? (Прогрессия)", "target_node": "progression" },
        { "index": 3, "text": "Проверь частоты резонанса.", "target_node": "resonance" },
        { "index": 4, "text": "[Завершить диалог]", "target_node": "EXIT" }
      ]
    },
    "greeting_injured": {
      "npc_text": "Критическая потеря био-материала! Стабилизирую твои каналы... Установи искусственные тромбоциты (Cyberware), чтобы не истекать кровью в бою.",
      "sound_event": "arcane_bridge:guide.greeting_injured",
      "options": [
        { "index": 1, "text": "Открыть журнал задач (Квестбук)", "target_node": "OPEN_FTB_QUESTS" },
        { "index": 2, "text": "Что делать дальше?", "target_node": "progression" },
        { "index": 3, "text": "[Завершить диалог]", "target_node": "EXIT" }
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