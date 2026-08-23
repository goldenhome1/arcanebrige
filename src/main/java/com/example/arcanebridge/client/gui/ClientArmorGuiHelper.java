package com.example.arcanebridge.client.gui;

import net.minecraft.client.Minecraft;

public class ClientArmorGuiHelper {
    public static void openRepairScreen() {
        Minecraft.getInstance().setScreen(new ArmorRepairScreen());
    }
}