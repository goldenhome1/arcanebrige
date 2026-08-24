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
                poseStack.translate(0.5D, 0.5D, 0.5D);
                poseStack.mulPose(Axis.XP.rotationDegrees(25));
                poseStack.mulPose(Axis.YP.rotationDegrees(45));
                poseStack.scale(1.25F, 1.25F, 1.25F);
            }
            case FIRST_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.55D, 0.25D, -0.45D);
                poseStack.mulPose(Axis.YP.rotationDegrees(35));
                poseStack.scale(0.70F, 0.70F, 0.70F);
            }
            case FIRST_PERSON_LEFT_HAND -> {
                poseStack.translate(-0.55D, 0.25D, -0.45D);
                poseStack.mulPose(Axis.YP.rotationDegrees(-35));
                poseStack.scale(0.70F, 0.70F, 0.70F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.0D, 0.20D, 0.05D);
                poseStack.scale(0.55F, 0.55F, 0.55F);
            }
            case GROUND -> {
                poseStack.translate(0.5D, 0.35D, 0.5D);
                poseStack.scale(0.65F, 0.65F, 0.65F);
            }
            case FIXED -> {
                poseStack.translate(0.5D, 0.5D, 0.5D);
                poseStack.scale(0.85F, 0.85F, 0.85F);
            }
            default -> {
                poseStack.scale(0.65F, 0.65F, 0.65F);
            }
        }

        super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}