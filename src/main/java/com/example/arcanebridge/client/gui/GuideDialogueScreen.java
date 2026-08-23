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
            int startY = this.height - 75;
            for (int i = 0; i < currentOptions.size(); i++) {
                int optionY = startY + (i * 12);
                if (mouseX >= 40 && mouseX <= this.width - 40 && mouseY >= optionY && mouseY <= optionY + 10) {
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
        // Полупрозрачная подложка снизу в стиле HUD
        int boxHeight = 110;
        int boxY = this.height - boxHeight - 10;
        
        graphics.fill(30, boxY, this.width - 30, this.height - 10, 0xCC0D0E11);
        graphics.fill(30, boxY, this.width - 30, boxY + 2, 0xFF5A4D41); // латунный верхний кант

        // Заголовок моба
        graphics.drawString(this.font, "§6[АРК-0 // ДИАГНОСТ]", 40, boxY + 8, 0xFFFFFF, false);

        // Текст реплики NPC
        graphics.drawString(this.font, "§f«" + this.currentNpcText + "»", 40, boxY + 22, 0xE0E0E0, false);

        // Список опций выбора (1, 2, 3...)
        int startY = boxY + 40;
        for (int i = 0; i < currentOptions.size(); i++) {
            DialogueOption opt = currentOptions.get(i);
            int optionY = startY + (i * 12);

            boolean isHovered = mouseX >= 40 && mouseX <= this.width - 40 && mouseY >= optionY && mouseY <= optionY + 10;
            String prefix = isHovered ? "§e► [" + opt.index() + "] " : "§7[" + opt.index() + "] §f";
            
            graphics.drawString(this.font, prefix + opt.text(), 40, optionY, 0xFFFFFF, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}