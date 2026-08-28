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

    private static final ResourceLocation DIALOGUE_RES = new ResourceLocation("arcane_bridge", "dialogues/guide_dialogues.json");

        public static void openGuideDialogue(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Entity entity = mc.level.getEntity(entityId);
        String jsonContent = loadDialogueJson();

                        // Определение динамического состояния игрока
        String startNode = "greeting";

                // Проверка наличия запечатанных скрижалей в руке для особого диалога
        if (mc.player.getMainHandItem().is(com.example.arcanebridge.item.ModItems.ANCIENT_SCROLL_PHASE.get())) {
            startNode = "decipher_phase_start";
        } else if (mc.player.getMainHandItem().is(com.example.arcanebridge.item.ModItems.ANCIENT_SCROLL_FLUID.get())) {
            startNode = "decipher_fluid_start";
        } else {
            boolean isInjured = mc.player.getHealth() <= 6.0F || mc.player.hasEffect(
                    ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("majruszsdifficulty", "bleeding"))
            );
            
            // Считываем реальные клиентские синхронизированные данные резонанса
            boolean hasDissonance = com.example.arcanebridge.network.ClientboundResonanceSyncPacket.clientStability < 70.0F ||
                                    com.example.arcanebridge.network.ClientboundResonanceSyncPacket.mechLoadStatic > com.example.arcanebridge.network.ClientboundResonanceSyncPacket.mechLimitStatic ||
                                    com.example.arcanebridge.network.ClientboundResonanceSyncPacket.arcaneLoadStatic > com.example.arcanebridge.network.ClientboundResonanceSyncPacket.arcaneLimitStatic ||
                                    com.example.arcanebridge.network.ClientboundResonanceSyncPacket.eleLoadStatic > com.example.arcanebridge.network.ClientboundResonanceSyncPacket.eleLimitStatic;

            if (isInjured) {
                startNode = "greeting_injured";
            } else if (hasDissonance) {
                startNode = "greeting_resonance_alert";
            }
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
              "npc_text": "Приветствую. Частоты эфира стабильны, помех не наблюдается. Чем могу помочь?",
              "sound_event": "arcane_bridge:guide.greeting_01",
              "options": [
                { "index": 1, "text": "Слухи об аномалиях и реликвиях в мире", "target_node": "rumors_hub" },
                { "index": 2, "text": "Кто ты такой? Расскажи о себе.", "target_node": "about_master" },
                { "index": 3, "text": "Открыть журнал задач (Квестбук)", "target_node": "OPEN_FTB_QUESTS" },
                { "index": 4, "text": "Что делать дальше? (Вектор прогрессии)", "target_node": "progression_hub" },
                { "index": 5, "text": "Спектральный Локатор структур", "target_node": "locator_hub" },
                { "index": 6, "text": "Проверь частоты резонанса.", "target_node": "resonance" },
                { "index": 7, "text": "[Завершить диалог]", "target_node": "EXIT" }
              ]
            },
                                    "rumors_hub": {
              "npc_text": "Сенсоры улавливают слабое эхо забытых сигналов в пространстве. О чем именно ты хочешь узнать?",
              "sound_event": "arcane_bridge:guide.rumors_hub",
              "options": [
                { "index": 1, "text": "Странный гул над кронами джунглей", "target_node": "hint_umvuthi_1" },
                { "index": 2, "text": "[Назад в меню]", "target_node": "greeting" }
              ]
            },
            "hint_umvuthi_1": {
              "npc_text": "Сенсоры ядра фиксируют устойчивый высокочастотный резонанс глубоко в тропических джунглях. Это не природная стихия и не простой механизм... Излучение пульсирует со строгой математической периодичностью.",
              "sound_event": "arcane_bridge:guide.hint_umvuthi_1",
              "options": [
                { "index": 1, "text": "[Далее...]", "target_node": "hint_umvuthi_2" },
                { "index": 2, "text": "[Назад к слухам]", "target_node": "rumors_hub" }
              ]
            },
            "hint_umvuthi_2": {
              "npc_text": "Старые хроники упоминали дикие племена, обосновавшиеся на верхушках гигантских деревьев. Они поклоняются пернатому исполину с сияющим нимбом за спиной, почитая его как божество. Но этот венец... судя по спектру, он поразительно напоминает фазовые апертуры древних Архитекторов.",
              "sound_event": "arcane_bridge:guide.hint_umvuthi_2",
              "options": [
                { "index": 1, "text": "[Далее...]", "target_node": "hint_umvuthi_3" },
                { "index": 2, "text": "[Назад]", "target_node": "hint_umvuthi_1" }
              ]
            },
            "hint_umvuthi_3": {
              "npc_text": "Если в том святилище действительно уцелели инженерные реликвии, там могут быть запечатанные скрижали калибровки. Если тебе удастся найти подобную схему — возьми её в руку и покажи мне. Возможно, нам удастся разгадать утерянный принцип.",
              "sound_event": "arcane_bridge:guide.hint_umvuthi_3",
              "options": [
                { "index": 1, "text": "[Назад к списку слухов]", "target_node": "rumors_hub" },
                { "index": 2, "text": "[Вернуться в главное меню]", "target_node": "greeting" }
              ]
            },
            "decipher_phase_start": {
              "npc_text": "Постой... Что это у тебя в руках? Невероятно! Ты добыл сервисные скрижали из святилища Пернатого Ретранслятора — Умвути! Дикари поклонялись ему как богу солнца, но взгляни на эти схемы: его сияющий венец был прототипом фазовой линзы, передававшей кинетическое вращение в эфирные колебания.",
              "sound_event": "arcane_bridge:guide.decipher_phase_start",
              "options": [
                { "index": 1, "text": "Сможешь расшифровать эти чертежи?", "target_node": "decipher_phase_explain" },
                { "index": 2, "text": "[Назад в меню]", "target_node": "greeting" }
              ]
            },
            "decipher_phase_explain": {
              "npc_text": "Разумеется. Формулы описывают геометрию двух рун: Фазового Истока и Фазового Эха. Сейчас я синхронизирую древние уравнения с твоим Рунным Блокнотом. Отныне твои кинетические линии валов Create смогут передавать вращение и стресс сквозь пространство без единого провода и шестерни.",
              "sound_event": "arcane_bridge:guide.decipher_phase_explain",
              "options": [
                { "index": 1, "text": "§a[Синхронизировать скрижаль с блокнотом]§r", "target_node": "ACTION_DECIPHER" },
                { "index": 2, "text": "[Назад]", "target_node": "decipher_phase_start" }
              ]
            },
                        "about_master": {
              "npc_text": "Когда-то я посвятил жизнь изучению границ между кинетикой машин и чистой магией. Теперь моё сознание связано с матрицей Резонанса, и я проецирую этот образ через передатчик, чтобы направлять инженеров и магов.",
              "sound_event": "arcane_bridge:guide.about_master",
              "options": [
                { "index": 1, "text": "Почему ты выглядишь как киборг-голограмма?", "target_node": "about_appearance" },
                { "index": 2, "text": "Что такое Эфирный Резонанс на самом деле?", "target_node": "about_philosophy" },
                { "index": 3, "text": "Какую помощь ты можешь мне оказать?", "target_node": "about_help" },
                { "index": 4, "text": "[Назад в главное меню]", "target_node": "greeting" }
              ]
            },
            "about_appearance": {
              "npc_text": "Обычная биологическая плоть не выдерживает прямого контакта с высокочастотным эфиром. Нейроимпланты Cyberware удерживают стабильность разума, а оптический модуль позволяет видеть структуру пространства.",
              "sound_event": "arcane_bridge:guide.about_appearance",
              "options": [
                { "index": 1, "text": "[Назад к вопросам о тебе]", "target_node": "about_master" },
                { "index": 2, "text": "[Назад в главное меню]", "target_node": "greeting" }
              ]
            },
            "about_philosophy": {
              "npc_text": "Мир звучит на трёх частотах: Механика, Аркана и Стихии. Если ты надеваешь слишком много предметов одной природы без Настроечных Матриц — в теле возникает разрушительный шум. Моя цель — научить тебя гармонии.",
              "sound_event": "arcane_bridge:guide.about_philosophy",
              "options": [
                { "index": 1, "text": "[Назад к вопросам о тебе]", "target_node": "about_master" },
                { "index": 2, "text": "[Назад в главное меню]", "target_node": "greeting" }
              ]
            },
            "about_help": {
              "npc_text": "Я сканирую спектральные сигналы древних подземелий, стабилизирую твои каналы при ранениях, а ночью могу развернуть защитное силовое поле вокруг ядра.",
              "sound_event": "arcane_bridge:guide.about_help",
              "options": [
                { "index": 1, "text": "[Назад к вопросам о тебе]", "target_node": "about_master" },
                { "index": 2, "text": "[Назад в главное меню]", "target_node": "greeting" }
              ]
            },
            "greeting_injured": {
              "npc_text": "Критическая потеря био-материала! Стабилизирую твои каналы... Установи искусственные тромбоциты (Cyberware), чтобы не истекать кровью в бою.",
              "sound_event": "arcane_bridge:guide.greeting_injured",
              "options": [
                { "index": 1, "text": "Кто ты такой?", "target_node": "about_master" },
                { "index": 2, "text": "Открыть журнал задач (Квестбук)", "target_node": "OPEN_FTB_QUESTS" },
                { "index": 3, "text": "Спектральный Локатор структур", "target_node": "locator_hub" },
                { "index": 4, "text": "Что делать дальше?", "target_node": "progression_hub" },
                { "index": 5, "text": "[Завершить диалог]", "target_node": "EXIT" }
              ]
            },
            "greeting_resonance_alert": {
              "npc_text": "Фиксирую частотный диссонанс! Твоя эфирная броня конфликтует. Установи Настроечную Матрицу, пока стабильность не упала.",
              "sound_event": "arcane_bridge:guide.resonance_warning",
              "options": [
                { "index": 1, "text": "Как снизить диссонанс?", "target_node": "resonance" },
                { "index": 2, "text": "Кто ты такой?", "target_node": "about_master" },
                { "index": 3, "text": "Открыть журнал задач (Квестбук)", "target_node": "OPEN_FTB_QUESTS" },
                { "index": 4, "text": "[Завершить диалог]", "target_node": "EXIT" }
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
                { "index": 1, "text": "Подземелье Кователя (Wrought Chamber)", "target_node": "ACTION_LOCATE:mowziesmobs:wrought_chamber" },
                { "index": 2, "text": "Древняя Фабрика (Ancient Factory / Harbinger)", "target_node": "ACTION_LOCATE:cataclysm:ancient_factory" },
                { "index": 3, "text": "Пылающая Арена (Burning Arena / Ignis)", "target_node": "ACTION_LOCATE:cataclysm:burning_arena" },
                { "index": 4, "text": "[Назад]", "target_node": "greeting" }
              ]
            },
                        "resonance": {
              "npc_text": "Следи за нагрузкой частот. Ношение более 2 предметов одной категории (Mech / Arcane / Ele) без Матрицы снижает стабильность тела.",
              "sound_event": "arcane_bridge:guide.resonance_info",
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