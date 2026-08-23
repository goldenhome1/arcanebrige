package com.example.arcanebridge.mixin;

import com.example.arcanebridge.logic.CyberwareHelper;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GogglesItem.class, remap = false)
public class GogglesItemMixin {

    @Inject(method = "isWearingGoggles", at = @At("HEAD"), cancellable = true)
    private static void arcane$cyberwareGogglesVision(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player != null) {
            if (CyberwareHelper.isCyberwareHudActive(player)) {
                cir.setReturnValue(true);
            }
        }
    }
}