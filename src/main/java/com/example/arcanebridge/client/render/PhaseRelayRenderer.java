package com.example.arcanebridge.client.render;

import com.example.arcanebridge.block.entity.PhaseRelayBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class PhaseRelayRenderer extends KineticBlockEntityRenderer<PhaseRelayBlockEntity> {

    public PhaseRelayRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(PhaseRelayBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        // 1. Отрисовка нативного вращающегося вала Create
        renderRotatingBuffer(be, getRotatedModel(be), ms, buffer.getBuffer(RenderType.solid()), light);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(PhaseRelayBlockEntity be) {
        return CachedBufferer.partialFacing(AllPartialModels.SHAFT, be.getBlockState());
    }
}