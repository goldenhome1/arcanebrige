package com.example.arcanebridge.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HudSettingsScreen extends Screen {

    public HudSettingsScreen() {
        super(Component.literal("§b[HUD-разъем / HUD Jack] Управление Визором"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 75;
        int buttonWidth = 240;
        int buttonHeight = 20;

        // 1. Оверлей Резонанса
        this.addRenderableWidget(Button.builder(
                getButtonText("Монитор Резонанса", HudConfig.showResonanceHud),
                btn -> {
                    HudConfig.showResonanceHud = !HudConfig.showResonanceHud;
                    btn.setMessage(getButtonText("Монитор Резонанса", HudConfig.showResonanceHud));
                }
        ).bounds(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight).build());

        // 2. Сканер HP (Neat)
        this.addRenderableWidget(Button.builder(
                getButtonText("Сканер HP мобов (Neat)", HudConfig.showHealthBars),
                btn -> {
                    HudConfig.showHealthBars = !HudConfig.showHealthBars;
                    btn.setMessage(getButtonText("Сканер HP мобов (Neat)", HudConfig.showHealthBars));
                }
        ).bounds(centerX - buttonWidth / 2, startY + 24, buttonWidth, buttonHeight).build());

        // 3. Индикация Урона
        this.addRenderableWidget(Button.builder(
                getButtonText("Индикация Урона (Damage Numbers)", HudConfig.showDamageNumbers),
                btn -> {
                    HudConfig.showDamageNumbers = !HudConfig.showDamageNumbers;
                    btn.setMessage(getButtonText("Индикация Урона (Damage Numbers)", HudConfig.showDamageNumbers));
                }
        ).bounds(centerX - buttonWidth / 2, startY + 48, buttonWidth, buttonHeight).build());

        // 4. Оптическая Карта
        this.addRenderableWidget(Button.builder(
                getButtonText("Оптическая Карта (Xaero)", HudConfig.showMap),
                btn -> {
                    HudConfig.showMap = !HudConfig.showMap;
                    btn.setMessage(getButtonText("Оптическая Карта (Xaero)", HudConfig.showMap));
                }
        ).bounds(centerX - buttonWidth / 2, startY + 72, buttonWidth, buttonHeight).build());

        // 5. Анализатор Блоков (Jade)
        this.addRenderableWidget(Button.builder(
                getButtonText("Анализатор Блоков (Jade)", HudConfig.showBlockInfo),
                btn -> {
                    HudConfig.showBlockInfo = !HudConfig.showBlockInfo;
                    btn.setMessage(getButtonText("Анализатор Блоков (Jade)", HudConfig.showBlockInfo));
                }
        ).bounds(centerX - buttonWidth / 2, startY + 96, buttonWidth, buttonHeight).build());

        // Кнопка сохранения
        this.addRenderableWidget(Button.builder(
                Component.literal("§cСохранить и Закрыть"),
                btn -> this.onClose()
        ).bounds(centerX - 100, startY + 128, 200, 20).build());
    }

    private Component getButtonText(String name, boolean state) {
        String status = state ? "§a[АКТИВЕН]" : "§c[ОТКЛЮЧЕН]";
        return Component.literal(name + ": " + status);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 95, 0x00F5FF);
        super.render(g, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}