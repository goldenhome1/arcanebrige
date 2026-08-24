package com.example.arcanebridge.client.render.item;

import com.example.arcanebridge.client.model.item.GuideCoreItemModel;
import com.example.arcanebridge.item.GuideCoreItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GuideCoreItemRenderer extends GeoItemRenderer<GuideCoreItem> {
    public GuideCoreItemRenderer() {
        super(new GuideCoreItemModel());
    }
}