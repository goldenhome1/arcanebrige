package com.example.arcanebridge.client.render.item;

import com.example.arcanebridge.client.model.item.GuideCoreItemModel;
import com.example.arcanebridge.item.GuideCoreItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GuideCoreItemRenderer extends GeoItemRenderer<GuideCoreItem> {

    public GuideCoreItemRenderer() {
        super(new GuideCoreItemModel());
    }

    @Override
    public RenderType getRenderType(GuideCoreItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        switch (transformType) {
            case GUI -> {
                // Точные координаты центра слота (0.5, 0.5) и оптимальный масштаб +30%
                poseStack.translate(0.5D, 0.5D, 0.0D);
                poseStack.mulPose(Axis.XP.rotationDegrees(25.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(1.30F, 1.30F, 1.30F);
            }
            case FIRST_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.45D, 0.20D, -0.45D);
                poseStack.mulPose(Axis.YP.rotationDegrees(40.0F));
                poseStack.scale(0.65F, 0.65F, 0.65F);
            }
            case FIRST_PERSON_LEFT_HAND -> {
                poseStack.translate(-0.45D, 0.20D, -0.45D);
                poseStack.mulPose(Axis.YP.rotationDegrees(-40.0F));
                poseStack.scale(0.65F, 0.65F, 0.65F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.0D, 0.15D, 0.05D);
                poseStack.scale(0.50F, 0.50F, 0.50F);
            }
            case GROUND -> {
                poseStack.translate(0.5D, 0.25D, 0.5D);
                poseStack.scale(0.55F, 0.55F, 0.55F);
            }
            case FIXED -> {
                poseStack.translate(0.5D, 0.5D, 0.5D);
                poseStack.scale(0.75F, 0.75F, 0.75F);
            }
            default -> {
                poseStack.scale(0.60F, 0.60F, 0.60F);
            }
        }

        super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}