package com.example.arcanebridge.client.render;

import com.example.arcanebridge.block.entity.PhaseRelayBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class PhaseRelayRenderer extends KineticBlockEntityRenderer<PhaseRelayBlockEntity> {

    public PhaseRelayRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(PhaseRelayBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        BlockState state = be.getBlockState();
        renderRotatingBuffer(be, getRotatedModel(be, state), ms, buffer.getBuffer(RenderType.solid()), light);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(PhaseRelayBlockEntity be, BlockState state) {
        Direction.Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);
        Direction facing = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        
        // Поворачиваем вертикальный вал AllPartialModels.SHAFT строго по направлению оси блока
        return CachedBuffers.partialFacingVertical(AllPartialModels.SHAFT, state, facing);
    }
}