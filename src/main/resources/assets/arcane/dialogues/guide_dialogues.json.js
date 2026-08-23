{
  "start_node": "greeting",
  "nodes": {
    "greeting": {
      "npc_text": "Приветствую, странник. Эфирные потоки спокойны, готов ответить на твои вопросы.",
      "sound_event": "arcane_bridge:guide.greeting_01",
      "options": [
        {
          "index": 1,
          "text": "Что делать дальше?",
          "target_node": "progression"
        },
        {
          "index": 2,
          "text": "Проверь частоты резонанса.",
          "target_node": "resonance"
        },
        {
          "index": 3,
          "text": "[Завершить диалог]",
          "target_node": "EXIT"
        }
      ]
    },
    "progression": {
      "npc_text": "Для стабилизации разлома и выхода в Незер тебе понадобится Эфирный Инициатор. Собери его на конвейере Create.",
      "sound_event": "arcane_bridge:guide.progression_nether",
      "options": [
        {
          "index": 1,
          "text": "[Назад]",
          "target_node": "greeting"
        },
        {
          "index": 2,
          "text": "[Завершить диалог]",
          "target_node": "EXIT"
        }
      ]
    },
    "resonance": {
      "npc_text": "Следи за нагрузкой частот. Ношение более 2 предметов одной категории без Матрицы вызовет диссонанс.",
      "sound_event": "arcane_bridge:guide.resonance_warning",
      "options": [
        {
          "index": 1,
          "text": "[Назад]",
          "target_node": "greeting"
        },
        {
          "index": 2,
          "text": "[Завершить диалог]",
          "target_node": "EXIT"
        }
      ]
    }
  }
}