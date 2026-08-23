package com.example.arcanebridge.client.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class GuideDialogueScreen extends Screen {

    private final Entity guideEntity;
    private JsonObject dialogueTree;
    private String currentNodeKey = "greeting";

            @Override
    public void tick() {
        super.tick();
        if (this.guideEntity instanceof net.minecraft.world.entity.Mob mob && this.minecraft != null && this.minecraft.player != null) {
            // Передаем управление ванильному контроллеру взгляда, исключая конфликт с GeckoLib
            mob.getLookControl().setLookAt(this.minecraft.player, 100.0F, 100.0F);

            double dx = this.minecraft.player.getX() - mob.getX();
            double dz = this.minecraft.player.getZ() - mob.getZ();
            float yaw = (float) (net.minecraft.util.Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            mob.setYRot(yaw);
            mob.yBodyRot = yaw;
        }
    }

    private int getBoxY(int npcLinesCount) {
        int optionsCount = Math.max(1, this.currentOptions.size());
        int boxHeight = 36 + (npcLinesCount * 11) + (optionsCount * 14) + 8;
        return this.height - boxHeight - 12;
    }

    private int getOptionY(int index, int boxY, int npcLinesCount) {
        int startY = boxY + 28 + (npcLinesCount * 11);
        return startY + (index * 14);
    }

    private String currentNpcText = "";
    private final List<DialogueOption> currentOptions = new ArrayList<>();

    public record DialogueOption(int index, String text, String targetNode) {}

    public GuideDialogueScreen(Entity guideEntity, String jsonContent) {
        super(Component.literal("ARC-0 Dialogue"));
        this.guideEntity = guideEntity;
        try {
            this.dialogueTree = JsonParser.parseString(jsonContent).getAsJsonObject();
            loadNode(this.dialogueTree.has("start_node") ? this.dialogueTree.get("start_node").getAsString() : "greeting");
        } catch (Exception e) {
            this.currentNpcText = "Ошибка загрузки диалоговой матрицы.";
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

        public void loadNode(String nodeKey) {
        if ("EXIT".equalsIgnoreCase(nodeKey) || this.dialogueTree == null) {
            this.onClose();
            return;
        }

                // Нативное открытие FTB Quests
        if ("OPEN_FTB_QUESTS".equalsIgnoreCase(nodeKey)) {
            this.onClose();
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.connection.sendCommand("ftbquests open_gui");
            }
            return;
        }

        // Запуск спектральной локации структур
        if (nodeKey.startsWith("ACTION_LOCATE:")) {
            String structureId = nodeKey.substring("ACTION_LOCATE:".length());
            this.onClose();
            if (this.guideEntity != null) {
                com.example.arcanebridge.network.ModMessages.sendToServer(
                        new com.example.arcanebridge.network.ServerboundGuideActionPacket(
                                this.guideEntity.getId(), "LOCATE", structureId
                        )
                );
            }
            return;
        }

        JsonObject nodes = this.dialogueTree.getAsJsonObject("nodes");
        if (nodes == null || !nodes.has(nodeKey)) {
            this.onClose();
            return;
        }

        this.currentNodeKey = nodeKey;
        JsonObject node = nodes.getAsJsonObject(nodeKey);

        this.currentNpcText = node.has("npc_text") ? node.get("npc_text").getAsString() : "";
        this.currentOptions.clear();

        if (node.has("options")) {
            var optionsArr = node.getAsJsonArray("options");
            for (var elem : optionsArr) {
                JsonObject opt = elem.getAsJsonObject();
                int idx = opt.get("index").getAsInt();
                String text = opt.get("text").getAsString();
                String target = opt.get("target_node").getAsString();
                this.currentOptions.add(new DialogueOption(idx, text, target));
            }
        }

        // Проигрывание озвучки реплики
        if (node.has("sound_event")) {
            playVoiceLine(node.get("sound_event").getAsString());
        }
    }

    private void playVoiceLine(String soundEventId) {
        ResourceLocation loc = new ResourceLocation(soundEventId);
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(loc);
        if (sound != null && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().getSoundManager().stop(); // глушим прошлую реплику
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(sound, 1.0F, 1.0F)
            );
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Цифры 1-9 (номера реплик)
        if (keyCode >= 49 && keyCode <= 57) {
            int selectedIndex = keyCode - 48;
            for (DialogueOption opt : this.currentOptions) {
                if (opt.index() == selectedIndex) {
                    selectOption(opt);
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override

    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (button == 0) {

            var npcLines = this.font.split(Component.literal("«" + this.currentNpcText + "»"), this.width - 90);

            int boxY = getBoxY(npcLines.size());


            for (int i = 0; i < currentOptions.size(); i++) {

                int optionY = getOptionY(i, boxY, npcLines.size());

                if (mouseX >= 36 && mouseX <= this.width - 36 && mouseY >= optionY - 2 && mouseY <= optionY + 11) {

                    selectOption(currentOptions.get(i));

                    return true;

                }

            }

        }

        return super.mouseClicked(mouseX, mouseY, button);

    }


    private void selectOption(DialogueOption option) {

        if (this.minecraft != null && this.minecraft.player != null) {

            this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 0.5F, 1.2F);

        }

        loadNode(option.targetNode());

    }


    @Override

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        var npcLines = this.font.split(Component.literal("«" + this.currentNpcText + "»"), this.width - 90);

        int boxY = getBoxY(npcLines.size());


        // Полупрозрачная подложка снизу в стиле HUD

        graphics.fill(30, boxY, this.width - 30, this.height - 12, 0xDD0D0E11);

        graphics.fill(30, boxY, this.width - 30, boxY + 2, 0xFF7A6855); // латунный кант


        // Заголовок моба

        graphics.drawString(this.font, "§6[Гид]", 42, boxY + 8, 0xFFFFFF, false);


        // Текст реплики NPC с динамическим переносом строк

        int textY = boxY + 22;

        for (var line : npcLines) {

            graphics.drawString(this.font, line, 42, textY, 0xEAEAEA, false);

            textY += 11;

        }


        // Список опций выбора с динамическим смещением и подсветкой кликабельной зоны

        for (int i = 0; i < currentOptions.size(); i++) {

            DialogueOption opt = currentOptions.get(i);

            int optionY = getOptionY(i, boxY, npcLines.size());


            boolean isHovered = mouseX >= 36 && mouseX <= this.width - 36 && mouseY >= optionY - 2 && mouseY <= optionY + 11;

            if (isHovered) {

                graphics.fill(36, optionY - 2, this.width - 36, optionY + 11, 0x25FFFFFF);

            }


            String prefix = isHovered ? "§e► [" + opt.index() + "] " : "§7[" + opt.index() + "] §f";

            graphics.drawString(this.font, prefix + opt.text(), 42, optionY, 0xFFFFFF, false);

        }


        super.render(graphics, mouseX, mouseY, partialTick);

    }
}