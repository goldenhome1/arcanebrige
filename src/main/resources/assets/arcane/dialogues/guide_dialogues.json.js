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
        { "index": 1, "text": "Где найти компоненты для Инициатора?", "target_node": "boss_hints" },
        { "index": 2, "text": "Как собрать сборочную линию Create?", "target_node": "assembly_tips" },
        { "index": 3, "text": "[Назад]", "target_node": "greeting" }
      ]
    },
    "boss_hints": {
      "npc_text": "Тебе понадобятся: Стальной Остов (Wroughtnaut), Аметистовый Резонатор (Nameless Guardian) и Термо-Ядро (Mechasent в рейдах).",
      "sound_event": "arcane_bridge:guide.boss_hints",
      "options": [
        { "index": 1, "text": "Тактика: Ferrous Wroughtnaut", "target_node": "tactics_wroughtnaut" },
        { "index": 2, "text": "Тактика: Nameless Guardian", "target_node": "tactics_nameless" },
        { "index": 3, "text": "[Назад]", "target_node": "progression_hub" }
      ]
    },
    "tactics_wroughtnaut": {
      "npc_text": "Кователь неуязвим для лобовых атак. Дождись, пока его топор застрянет в земле, и атакуй обнаженное сочленение сзади.",
      "sound_event": "arcane_bridge:guide.tactics_wroughtnaut",
      "options": [
        { "index": 1, "text": "[Назад]", "target_node": "boss_hints" }
      ]
    },
    "tactics_nameless": {
      "npc_text": "Страж защищен кинетическим щитом. Сбивай его энергетические сферы дальнобойными заклинаниями Hex Casting или орудиями CBC.",
      "sound_event": "arcane_bridge:guide.tactics_nameless",
      "options": [
        { "index": 1, "text": "[Назад]", "target_node": "boss_hints" }
      ]
    },
    "assembly_tips": {
      "npc_text": "Установи конвейер: Остов -> Деплоер (Резонатор) -> Дозатор (250мБ жидкого опыта) -> Деплоер (Термо-Ядро) -> Пресс.",
      "sound_event": "arcane_bridge:guide.assembly_tips",
      "options": [
        { "index": 1, "text": "[Назад]", "target_node": "progression_hub" }
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