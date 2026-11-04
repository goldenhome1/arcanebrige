package com.example.arcanebridge.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;

@Pseudo
@Mixin(targets = "com.msdoggirl.dglib.SkinSwapper", remap = false)
public class DGLibSkinSwapperMixin {

    @Inject(method = "backupOriginalSkin", at = @At("HEAD"), cancellable = true, remap = false)
    private static void arcane$backupOriginalSkin(AbstractClientPlayer player, ResourceLocation skinLoc, CallbackInfoReturnable<NativeImage> cir) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;

            TextureManager tm = mc.getTextureManager();
            ResourceLocation targetLoc = skinLoc;
            if (targetLoc == null && player != null) {
                targetLoc = player.getSkinTextureLocation();
            }
            if (targetLoc == null) return;

            AbstractTexture texture = tm.getTexture(targetLoc);

            // 1. If it's a DynamicTexture with pixels in memory (CustomSkinLoader / runtime skins)
            if (texture instanceof DynamicTexture dynamicTexture && dynamicTexture.getPixels() != null) {
                NativeImage src = dynamicTexture.getPixels();
                NativeImage copy = new NativeImage(src.format(), src.getWidth(), src.getHeight(), true);
                copy.copyFrom(src);
                cir.setReturnValue(copy);
                return;
            }

            // 2. If it's HttpTexture and file is already cached on disk
            if (texture instanceof HttpTexture) {
                try {
                    Field fileField = HttpTexture.class.getDeclaredField("file");
                    fileField.setAccessible(true);
                    File file = (File) fileField.get(texture);
                    if (file != null && file.exists()) {
                        try (FileInputStream fis = new FileInputStream(file)) {
                            NativeImage img = NativeImage.read(fis);
                            if (img != null) {
                                cir.setReturnValue(img);
                                return;
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }

            // 3. Download directly from GPU / OpenGL texture memory
            if (texture != null && texture.getId() > 0 && RenderSystem.isOnRenderThread()) {
                try {
                    RenderSystem.bindTexture(texture.getId());
                    NativeImage image = new NativeImage(NativeImage.Format.RGBA, 64, 64, true);
                    image.downloadTexture(0, false);
                    cir.setReturnValue(image);
                    return;
                } catch (Throwable ignored) {
                }
            }

            // 4. Fallback: try Minotar with player username (for offline mode servers with real nicknames)
            if (player != null && player.getGameProfile() != null && player.getGameProfile().getName() != null) {
                String username = player.getGameProfile().getName();
                try {
                    URL url = new URL("https://minotar.net/skin/" + username);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(5000);
                    if (conn.getResponseCode() == 200) {
                        try (InputStream in = conn.getInputStream()) {
                            NativeImage img = NativeImage.read(in);
                            if (img != null) {
                                cir.setReturnValue(img);
                                return;
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
            // Let the original method handle fallback if our capture fails
        }
    }
}
