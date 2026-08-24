package com.example.arcanebridge.client.model;

import com.example.arcanebridge.entity.ArcaneGuideEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class ArcaneGuideModel extends GeoModel<ArcaneGuideEntity> {

    @Override
    public ResourceLocation getModelResource(ArcaneGuideEntity animatable) {
        return new ResourceLocation("arcane_bridge", "geo/arcane_guide.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ArcaneGuideEntity animatable) {
        return new ResourceLocation("arcane_bridge", "textures/entity/arcane_guide.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ArcaneGuideEntity animatable) {
        return new ResourceLocation("arcane_bridge", "animations/arcane_guide.animation.json");
    }

        @Override
    public void setCustomAnimations(ArcaneGuideEntity animatable, long instanceId, AnimationState<ArcaneGuideEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        // Скрываем кость магических кругов, пока не идет анимация каста
        CoreGeoBone magicCircle = getAnimationProcessor().getBone("magic circle");
        if (magicCircle != null) {
            magicCircle.setHidden(!animatable.isCasting());
        }

                // Поворот головы за игроком (Pitch / Yaw) вне фазы каста
        CoreGeoBone head = getAnimationProcessor().getBone("Head");
        if (head != null && !animatable.isCasting()) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            if (entityData != null) {
                head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
                head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
            }
        }

        // Цифровой глитч-эффект (горизонтальный джиттер и слайсинг)
        int animState = animatable.getAnimState();
        if (animState == ArcaneGuideEntity.STATE_MATERIALIZE || animState == ArcaneGuideEntity.STATE_DEMATERIALIZE) {
            float time = animatable.tickCount + animationState.getPartialTick();
            float jitterStrength = animState == ArcaneGuideEntity.STATE_DEMATERIALIZE ? 0.35F : 0.20F;

            CoreGeoBone body = getAnimationProcessor().getBone("body2");
            if (body != null) {
                float glitchX = (float) Math.sin(time * 15.0F) * (float) Math.cos(time * 23.0F) * jitterStrength;
                float glitchZ = (float) Math.cos(time * 19.0F) * jitterStrength;
                body.setPosX(body.getPosX() + glitchX * 16.0F);
                body.setPosZ(body.getPosZ() + glitchZ * 16.0F);

                float scaleGlitch = 1.0F + (float) Math.sin(time * 30.0F) * 0.15F;
                body.setScaleX(scaleGlitch);
                body.setScaleZ(scaleGlitch);
            }

            if (head != null) {
                float headGlitchX = (float) Math.cos(time * 27.0F) * jitterStrength * 1.5F;
                head.setPosX(head.getPosX() + headGlitchX * 16.0F);
            }
        }
    }
}