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
    }
}