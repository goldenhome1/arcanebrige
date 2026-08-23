package com.example.arcanebridge.client.renderer;

import com.example.arcanebridge.client.model.ArcaneGuideModel;
import com.example.arcanebridge.client.render.ShieldDomeLayer;
import com.example.arcanebridge.entity.ArcaneGuideEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ArcaneGuideRenderer extends GeoEntityRenderer<ArcaneGuideEntity> {
    public ArcaneGuideRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ArcaneGuideModel());
        this.shadowRadius = 0.5F;
        this.addRenderLayer(new ShieldDomeLayer(this));
    }

    @Override
    public boolean shouldRender(ArcaneGuideEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        if (entity.getAnimState() == ArcaneGuideEntity.STATE_SHIELD_NIGHT) {
            return true;
        }
        return super.shouldRender(entity, frustum, camX, camY, camZ);
    }
}