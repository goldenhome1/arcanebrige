package com.example.arcanebridge.client.gui;

import com.example.arcanebridge.network.NetworkHandler;
import com.example.arcanebridge.network.ServerboundRepairCompletePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ArmorRepairScreen extends Screen {

    private static final Random RANDOM = new Random();
    private final int gameType; // 0 = Манометр, 1 = Реле предохранителей, 2 = Сопряжение маховика

    // --- Тип 0: Pressure Valve ---
    private float needlePos = 0.0f;
    private float needleSpeed = 0.032f;
    private int valveHitsRequired = 2;
    private int currentValveHits = 0;
    private Button valveButton;

    // --- Тип 1: Circuit Relays ---
    private final List<Integer> relaySequence = new ArrayList<>();
    private int nextExpectedRelay = 1;
    private final boolean[] relayActivated = new boolean[4];

    // --- Тип 2: Gear Alignment ---
    private float gearAngle = 0.0f;
    private Button gearButton;

    public ArmorRepairScreen() {
        super(Component.literal("Полевая калибровка сервоприводов"));
        this.gameType = RANDOM.nextInt(3);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        if (gameType == 0) {
            valveButton = this.addRenderableWidget(Button.builder(Component.literal("§6[ СБРОС ДАВЛЕНИЯ (ПРОБЕЛ) ]"), btn -> {
                handleValveAction();
            }).bounds(cx - 90, cy + 32, 180, 20).build());
        } 
        else if (gameType == 1) {
            relaySequence.clear();
            for (int i = 1; i <= 4; i++) relaySequence.add(i);
            Collections.shuffle(relaySequence);

            int startX = cx - 110;
            for (int i = 0; i < 4; i++) {
                int relayNum = relaySequence.get(i);
                int btnIndex = i;
                this.addRenderableWidget(Button.builder(Component.literal("§bРеле #" + relayNum), btn -> {
                    if (relayNum == nextExpectedRelay) {
                        relayActivated[btnIndex] = true;
                        btn.active = false;
                        btn.setMessage(Component.literal("§a✔ #" + relayNum));
                        nextExpectedRelay++;
                        playUiSound(SoundEvents.NOTE_BLOCK_PLING, 1.0f + (relayNum * 0.25f));

                        if (nextExpectedRelay > 4) {
                            completeTask();
                        }
                    } else {
                        playUiSound(SoundEvents.DISPENSER_FAIL, 0.6f);
                        nextExpectedRelay = 1;
                        this.clearWidgets();
                        this.init();
                    }
                }).bounds(startX + (i * 58), cy + 10, 52, 24).build());
            }
        } 
        else if (gameType == 2) {
            gearButton = this.addRenderableWidget(Button.builder(Component.literal("§e[ ЗАФИКСИРОВАТЬ (ПРОБЕЛ) ]"), btn -> {
                handleGearAction();
            }).bounds(cx - 95, cy + 38, 190, 20).build());
        }
    }

    private void handleValveAction() {
        if (needlePos >= 0.38f && needlePos <= 0.62f) {
            currentValveHits++;
            playUiSound(SoundEvents.NOTE_BLOCK_BELL, 1.5f);
            if (currentValveHits >= valveHitsRequired) {
                completeTask();
            }
        } else {
            playUiSound(SoundEvents.FIRE_EXTINGUISH, 0.8f);
            currentValveHits = 0;
        }
    }

    private void handleGearAction() {
        float normalizedAngle = (gearAngle % 360.0f);
        if (normalizedAngle >= 155.0f && normalizedAngle <= 205.0f) {
            playUiSound(SoundEvents.ANVIL_USE, 1.2f);
            completeTask();
        } else {
            playUiSound(SoundEvents.ITEM_BREAK, 0.7f);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            if (gameType == 0) {
                handleValveAction();
                return true;
            } else if (gameType == 2) {
                handleGearAction();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void completeTask() {
        NetworkHandler.sendToServer(new ServerboundRepairCompletePacket());
        this.onClose();
    }

    private void playUiSound(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, pitch)
        );
    }

    private void playUiSound(Holder<SoundEvent> soundHolder, float pitch) {
        if (soundHolder != null) {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(soundHolder.value(), pitch)
            );
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (gameType == 0) {
            needlePos += needleSpeed;
            if (needlePos >= 1.0f || needlePos <= 0.0f) {
                needleSpeed = -needleSpeed;
            }
        } else if (gameType == 2) {
            gearAngle = (gearAngle + 5.0f) % 360.0f;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(g);

        int cx = this.width / 2;
        int cy = this.height / 2;
        int panelW = 260;
        int panelH = 130;

        g.fill(cx - panelW / 2, cy - panelH / 2, cx + panelW / 2, cy + panelH / 2, 0xEE111116);
        g.fill(cx - panelW / 2 - 1, cy - panelH / 2 - 1, cx + panelW / 2 + 1, cy - panelH / 2, 0xFF4A4A5A);
        g.fill(cx - panelW / 2 - 1, cy + panelH / 2, cx + panelW / 2 + 1, cy + panelH / 2 + 1, 0xFF4A4A5A);
        g.fill(cx - panelW / 2 - 1, cy - panelH / 2, cx - panelW / 2, cy + panelH / 2, 0xFF4A4A5A);
        g.fill(cx + panelW / 2, cy - panelH / 2, cx + panelW / 2 + 1, cy + panelH / 2, 0xFF4A4A5A);

        g.drawCenteredString(this.font, "§6⚙ ПОЛЕВАЯ ЮСТИРОВКА СЕРВОПРИВОДОВ", cx, cy - 54, 0xFFFFFF);

        if (gameType == 0) {
            g.drawCenteredString(this.font, "§7Попадите в §aзелёную зону§7 для сброса давления (" + currentValveHits + "/" + valveHitsRequired + ")", cx, cy - 38, 0xAAAAAA);

            int barW = 180;
            int barH = 14;
            int barX = cx - barW / 2;
            int barY = cy - 12;

            g.fill(barX, barY, barX + barW, barY + barH, 0xFF1A1A1A);

            int greenX1 = barX + (int) (barW * 0.38f);
            int greenX2 = barX + (int) (barW * 0.62f);
            g.fill(greenX1, barY, greenX2, barY + barH, 0xFF2E7D32);

            int needleX = barX + (int) (barW * needlePos);
            g.fill(needleX - 2, barY - 3, needleX + 2, barY + barH + 3, 0xFFFFCC00);

        } else if (gameType == 1) {
            g.drawCenteredString(this.font, "§7Включите реле по порядку: §e1 §7➔ §e2 §7➔ §e3 §7➔ §e4", cx, cy - 38, 0xAAAAAA);
            g.drawCenteredString(this.font, "§bОжидается переключение: §fРеле #" + nextExpectedRelay, cx, cy - 16, 0x00E5FF);

        } else if (gameType == 2) {
            float currentAng = (gearAngle % 360.0f);
            boolean inSector = currentAng >= 155.0f && currentAng <= 205.0f;
            int color = inSector ? 0xFF00FF00 : 0xFFFF3333;
            String status = inSector ? "§a[ ЗУБЬЯ СОПРЯЖЕНЫ ]" : "§c[ НЕСООСНОСТЬ ПРИВОДОВ ]";

            g.drawCenteredString(this.font, status, cx, cy - 38, color);

            int dialW = 160;
            int dialX = cx - dialW / 2;
            int dialY = cy - 10;
            g.fill(dialX, dialY, dialX + dialW, dialY + 12, 0xFF1A1A1A);

            int targetX1 = dialX + (int) (dialW * (155.0f / 360.0f));
            int targetX2 = dialX + (int) (dialW * (205.0f / 360.0f));
            g.fill(targetX1, dialY, targetX2, dialY + 12, 0xFF388E3C);

            int curX = dialX + (int) (dialW * (currentAng / 360.0f));
            g.fill(curX - 2, dialY - 3, curX + 2, dialY + 15, color);
        }

        super.render(g, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}