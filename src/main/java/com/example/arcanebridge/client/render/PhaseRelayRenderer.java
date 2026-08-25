package com.example.arcanebridge.client.render;

import com.example.arcanebridge.block.entity.PhaseRelayBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public class PhaseRelayRenderer extends KineticBlockEntityRenderer<PhaseRelayBlockEntity> {

    public PhaseRelayRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(PhaseRelayBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        BlockState state = be.getBlockState();
        renderRotatingBuffer(be, getRotatedModel(be, state), ms, buffer.getBuffer(RenderType.solid()), light);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(PhaseRelayBlockEntity be, BlockState state) {
        // Берем запеченную модель вала Create с правильной ориентацией оси (X, Y или Z)
        BlockState shaftState = AllBlocks.SHAFT.get().defaultBlockState()
                .setValue(RotatedPillarKineticBlock.AXIS, state.getValue(RotatedPillarKineticBlock.AXIS));
        return CachedBuffers.block(shaftState);
    }
}