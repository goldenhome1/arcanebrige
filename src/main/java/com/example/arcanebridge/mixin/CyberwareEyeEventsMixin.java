package com.example.arcanebridge.mixin;

import net.minecraftforge.client.event.ViewportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.maxwell.cyber_ware_port.client.upgrades.cybereye.EyeWareEvents", remap = false)
public class CyberwareEyeEventsMixin {

    @Inject(method = "onComputeFov", at = @At("HEAD"), cancellable = true, remap = false)
    private static void arcane$disableShiftZoom(ViewportEvent.ComputeFov event, CallbackInfo ci) {
        ci.cancel();
    }
}
