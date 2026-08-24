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

                // Послойная материализация снизу вверх / дематериализация сверху вниз
        int animState = animatable.getAnimState();
        if (animState == ArcaneGuideEntity.STATE_MATERIALIZE || animState == ArcaneGuideEntity.STATE_DEMATERIALIZE) {
            int totalDuration = (animState == ArcaneGuideEntity.STATE_MATERIALIZE) ? 30 : 25;
            int remaining = animatable.getActionTicks();
            float partial = animationState.getPartialTick();
            float current = (totalDuration - remaining) + partial;
            float rawProgress = Mth.clamp(current / (float) totalDuration, 0.0F, 1.0F);

            float progress = (animState == ArcaneGuideEntity.STATE_MATERIALIZE) ? rawProgress : (1.0F - rawProgress);
            float time = animatable.tickCount + partial;

            applyProgressiveHologram(progress, time);
        }
    }

    private void applyProgressiveHologram(float progress, float time) {
        CoreGeoBone rLeg = getAnimationProcessor().getBone("Right Leg");
        CoreGeoBone lLeg = getAnimationProcessor().getBone("Left Leg");
        CoreGeoBone body = getAnimationProcessor().getBone("body2");
        CoreGeoBone rArm = getAnimationProcessor().getBone("Right Arm");
        CoreGeoBone lArm = getAnimationProcessor().getBone("Left Arm");
        CoreGeoBone head = getAnimationProcessor().getBone("Head");

        // Фаза 1: Ноги (0.00 - 0.28)
        applyBoneSlice(rLeg, progress, 0.00F, 0.28F, time);
        applyBoneSlice(lLeg, progress, 0.00F, 0.28F, time);

        // Фаза 2: Корпус (0.28 - 0.60)
        applyBoneSlice(body, progress, 0.28F, 0.60F, time);

        // Фаза 3: Руки (0.55 - 0.82)
        applyBoneSlice(rArm, progress, 0.55F, 0.82F, time);
        applyBoneSlice(lArm, progress, 0.55F, 0.82F, time);

        // Фаза 4: Голова (0.80 - 1.00)
        applyBoneSlice(head, progress, 0.80F, 1.00F, time);
    }

    private void applyBoneSlice(CoreGeoBone bone, float progress, float start, float end, float time) {
        if (bone == null) return;

        if (progress < start) {
            // Слой еще не материализовался
            bone.setScaleY(0.001F);
            bone.setScaleX(0.001F);
            bone.setScaleZ(0.001F);
            bone.setPosX(0.0F);
            bone.setPosZ(0.0F);
        } else if (progress >= end) {
            // Слой полностью материализован
            bone.setScaleY(1.0F);
            bone.setScaleX(1.0F);
            bone.setScaleZ(1.0F);
            bone.setPosX(0.0F);
            bone.setPosZ(0.0F);
        } else {
            // Активная фаза сборки слоя со скан-глитчем
            float sliceProgress = (progress - start) / (end - start);
            float jitterX = (float) Math.sin(time * 25.0F) * 0.15F;
            float jitterZ = (float) Math.cos(time * 31.0F) * 0.15F;

            bone.setScaleY(Math.max(0.05F, sliceProgress));
            bone.setScaleX(1.0F + jitterX);
            bone.setScaleZ(1.0F + jitterZ);
            bone.setPosX(jitterX * 6.0F);
            bone.setPosZ(jitterZ * 6.0F);
        }
    }
}