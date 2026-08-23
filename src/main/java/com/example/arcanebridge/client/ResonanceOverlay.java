package com.example.arcanebridge.client;

import com.example.arcanebridge.logic.CyberwareHelper;
import com.example.arcanebridge.network.ClientboundResonanceSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Set;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "arcane_bridge")
public class ResonanceOverlay {

    private static final Set<String> ALLOWED_ITEMS = Set.of(
            "create:goggles",
            "create:infernal_goggles",
            "cyber_ware_port:hud_lens",
            "cyber_ware_port:hud_jack"
    );

    private static final int MAX_BAR_WIDTH = 40;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!HudConfig.showResonanceHud) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        if (!isGearEquipped(mc)) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenHeight = event.getWindow().getGuiScaledHeight();

        float stability = ClientboundResonanceSyncPacket.clientStability;
        int mLoad = ClientboundResonanceSyncPacket.mechLoadStatic;
        int aLoad = ClientboundResonanceSyncPacket.arcaneLoadStatic;
        int eLoad = ClientboundResonanceSyncPacket.eleLoadStatic;

        int mLimit = ClientboundResonanceSyncPacket.mechLimitStatic;
        int aLimit = ClientboundResonanceSyncPacket.arcaneLimitStatic;
        int eLimit = ClientboundResonanceSyncPacket.eleLimitStatic;

        boolean mShock = ClientboundResonanceSyncPacket.mechShockStatic;
        boolean aShock = ClientboundResonanceSyncPacket.arcaneShockStatic;
        boolean eShock = ClientboundResonanceSyncPacket.eleShockStatic;

        int x = 60;
        int y = screenHeight - 60;

        drawBar(guiGraphics, x, y, MAX_BAR_WIDTH, 4, stability, stability < 20 ? 0xFFFF0000 : 0xFF00E5FF);

        drawFrequencyIndicator(guiGraphics, x, y + 10, "M", mLoad, mLimit, 0xFFAA00, mShock);
        drawFrequencyIndicator(guiGraphics, x, y + 20, "A", aLoad, aLimit, 0xAA00FF, aShock);
        drawFrequencyIndicator(guiGraphics, x, y + 30, "E", eLoad, eLimit, 0x00AAFF, eShock);
    }

    private static void drawBar(GuiGraphics g, int x, int y, int w, int h, float val, int color) {
        int fill = (int) ((Math.min(val, 100.0f) / 100.0) * w);
        g.fill(x, y, x + w, y + h, 0xFF222222);
        g.fill(x, y, x + fill, y + h, color | 0xFF000000);
    }

    private static void drawFrequencyIndicator(GuiGraphics g, int x, int y, String label, int load, int limit, int color, boolean isShocked) {
        boolean critical = load > limit || isShocked;

        int actualColor = color;
        int alpha = 0xAA;

        if (isShocked) {
            actualColor = 0xFFFF0000;
            alpha = ((System.currentTimeMillis() / 100) % 2 == 0) ? 0xFF : 0x55;
        } else if (critical) {
            alpha = ((System.currentTimeMillis() / 250) % 2 == 0) ? 0xFF : 0xAA;
        }

        int finalColor = actualColor | (alpha << 24);

        g.drawString(Minecraft.getInstance().font, label, x - 10, y - 2, 0xFFFFFF, false);

        int fillWidth = Math.min(load * 5, MAX_BAR_WIDTH);

        g.fill(x, y, x + MAX_BAR_WIDTH, y + 2, 0xFF222222);
        g.fill(x, y, x + fillWidth, y + 2, finalColor);
    }

    private static boolean isGearEquipped(Minecraft mc) {
        Player player = mc.player;
        if (player == null) return false;

        ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (ALLOWED_ITEMS.contains(getItemId(headStack.getItem()))) return true;

        boolean hasCurio = CuriosApi.getCuriosInventory(player)
                .map(inv -> inv.findFirstCurio(stack -> ALLOWED_ITEMS.contains(getItemId(stack.getItem()))).isPresent())
                .orElse(false);

        if (hasCurio) return true;

        return CyberwareHelper.isCyberwareHudActive(player);
    }

    private static String getItemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }
}