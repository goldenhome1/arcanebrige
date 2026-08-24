package com.example.arcanebridge.client.model.item;

import com.example.arcanebridge.item.GuideCoreItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GuideCoreItemModel extends GeoModel<GuideCoreItem> {
    @Override
    public ResourceLocation getModelResource(GuideCoreItem animatable) {
        return new ResourceLocation("arcane_bridge", "geo/item/guide_core.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GuideCoreItem animatable) {
        return new ResourceLocation("arcane_bridge", "textures/item/guide_core.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GuideCoreItem animatable) {
        return new ResourceLocation("arcane_bridge", "animations/item/guide_core.animation.json");
    }
}