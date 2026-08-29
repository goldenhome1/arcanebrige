package com.example.arcanebridge.client.render;

import com.example.arcanebridge.block.PhaseFluidBlock;
import com.example.arcanebridge.block.entity.PhaseFluidBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class PhaseFluidRenderer implements BlockEntityRenderer<PhaseFluidBlockEntity> {

    private static final ResourceLocation TEX_END_SKY =
            new ResourceLocation("minecraft", "textures/environment/end_sky.png");
    private static final ResourceLocation GLYPH_TX =
            new ResourceLocation("arcane_bridge", "textures/vfx/phase_fluid_glyph_tx.png");
    private static final ResourceLocation GLYPH_RX =
            new ResourceLocation("arcane_bridge", "textures/vfx/phase_fluid_glyph_rx.png");
    private static final ResourceLocation GLYPH_LINES =
            new ResourceLocation("arcane_bridge", "textures/vfx/phase_fluid_glyph_lines.png");

    public PhaseFluidRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(PhaseFluidBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (state == null || !(state.getBlock() instanceof PhaseFluidBlock)) return;

        Direction.Axis axis = state.hasProperty(PhaseFluidBlock.AXIS) ? state.getValue(PhaseFluidBlock.AXIS) : Direction.Axis.Y;

        ms.pushPose();
        ms.translate(0.5D, 0.5D, 0.5D);

        // 1. Поворот по оси трубы Create
        switch (axis) {
            case X -> ms.mulPose(Axis.YP.rotationDegrees(90.0F));
            case Z -> {}
            case Y -> ms.mulPose(Axis.XP.rotationDegrees(90.0F));
        }

        float time = (be.getLevel() != null ? be.getLevel().getGameTime() : 0) + partialTicks;

        // -------------------------------------------------------------------------
        // 💧 1. ОТРИСОВКА ЖИДКОСТИ ВНУТРИ СТЕКЛЯННОЙ ТРУБЫ
        // -------------------------------------------------------------------------
        IFluidHandler storage = be.getFluidStorage();
        FluidStack fluid = storage.getFluidInTank(0);
        if (!fluid.isEmpty()) {
            renderFluidInside(ms, buffer, fluid, storage.getTankCapacity(0), light);
        }

        // -------------------------------------------------------------------------
        // 🌌 2. СТАТИЧНЫЕ НЕПРОЗРАЧНЫЕ ЗАГЛУШКИ ЭНДА (ПЕРЕКРЫВАЮТ СОСЕДНИЕ ТРУБЫ)
        // -------------------------------------------------------------------------
        // Точный размер внутреннего отверстия фланца Create (ровно 8x8 пикселей)
        float portalHalfSize = 0.245F;
        float portalDepth = 0.498F;

        VertexConsumer voidConsumer = buffer.getBuffer(RenderType.entitySolid(TEX_END_SKY));
        drawFixedPortalBore(ms.last().pose(), ms.last().normal(), voidConsumer, portalHalfSize, portalDepth);
        drawFixedPortalBore(ms.last().pose(), ms.last().normal(), voidConsumer, portalHalfSize, -portalDepth);

        // -------------------------------------------------------------------------
        // 🔮 3. ЕДИНЫЙ ВРАЩАЮЩИЙСЯ КАРКАС (ГЛИФЫ + БОКОВЫЕ ЛИНИИ)
        // -------------------------------------------------------------------------
        float pulse = 1.0F + (float) Math.sin(time * 0.08F) * 0.02F;
        float r = be.isReceiver ? 0.20F : 0.05F;
        float g = be.isReceiver ? 0.60F : 0.90F;
        float b = be.isReceiver ? 1.00F : 0.95F;
        float a = 0.95F;

        // Габариты строго подогнаны под углы стыковки
        float cageSize = 0.248F * pulse;
        float cageHalfLen = 0.500F;
        float rotAngle = time * 0.6F;

        ms.pushPose();
        // Вращаем весь рунический контур как единое целое
        ms.mulPose(Axis.ZP.rotationDegrees(rotAngle));

        Matrix4f cagePosMat = ms.last().pose();
        Matrix3f cageNormMat = ms.last().normal();

        // А. Торцевые вращающиеся глифы
        ResourceLocation glyphTexture = be.isReceiver ? GLYPH_RX : GLYPH_TX;
        VertexConsumer glyphConsumer = buffer.getBuffer(RenderType.entityTranslucent(glyphTexture));

        drawEndGlyph(cagePosMat, cageNormMat, glyphConsumer, cageSize, cageHalfLen + 0.001F, r, g, b, a, true);
        drawEndGlyph(cagePosMat, cageNormMat, glyphConsumer, cageSize, -(cageHalfLen + 0.001F), r, g, b, a, false);

        // Б. 4 боковые грани, соединяющие углы глифов от торца до торца
        VertexConsumer wallConsumer = buffer.getBuffer(RenderType.entityTranslucent(GLYPH_LINES));
        for (int i = 0; i < 4; i++) {
            ms.pushPose();
            ms.mulPose(Axis.ZP.rotationDegrees(i * 90.0F));
            ms.translate(0.0D, cageSize, 0.0D);

            Matrix4f wallPosMat = ms.last().pose();
            Matrix3f wallNormMat = ms.last().normal();

            drawWallPanel(wallPosMat, wallNormMat, wallConsumer, cageSize, cageHalfLen, r, g, b, a * 0.90F);
            ms.popPose();
        }

        ms.popPose();
        ms.popPose();
    }

    /**
     * Рендерит уровень текущей жидкости внутри трубы
     */
    private void renderFluidInside(PoseStack ms, MultiBufferSource buffer, FluidStack fluid, int capacity, int light) {
        IClientFluidTypeExtensions clientFluid = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation stillTex = clientFluid.getStillTexture(fluid);
        if (stillTex == null) return;

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTex);
        int color = clientFluid.getTintColor(fluid);

        float fR = (float) (color >> 16 & 255) / 255.0F;
        float fG = (float) (color >> 8 & 255) / 255.0F;
        float fB = (float) (color & 255) / 255.0F;
        float fA = (float) (color >> 24 & 255) / 255.0F;
        if (fA <= 0.01F) fA = 1.0F;

        float fillRatio = Math.min(1.0F, Math.max(0.12F, (float) fluid.getAmount() / (float) capacity));

        float xMin = -0.22F;
        float xMax = 0.22F;
        float yMin = -0.22F;
        float yMax = -0.22F + (0.44F * fillRatio);
        float zMin = -0.48F;
        float zMax = 0.48F;

        VertexConsumer builder = buffer.getBuffer(RenderType.translucent());
        Matrix4f mat = ms.last().pose();
        Matrix3f norm = ms.last().normal();

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        // Верхняя грань уровня жидкости
        builder.vertex(mat, xMin, yMax, zMin).color(fR, fG, fB, fA).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, 0, 1, 0).endVertex();
        builder.vertex(mat, xMin, yMax, zMax).color(fR, fG, fB, fA).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, 0, 1, 0).endVertex();
        builder.vertex(mat, xMax, yMax, zMax).color(fR, fG, fB, fA).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, 0, 1, 0).endVertex();
        builder.vertex(mat, xMax, yMax, zMin).color(fR, fG, fB, fA).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, 0, 1, 0).endVertex();

        // Нижняя грань
        builder.vertex(mat, xMin, yMin, zMax).color(fR, fG, fB, fA).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, 0, -1, 0).endVertex();
        builder.vertex(mat, xMin, yMin, zMin).color(fR, fG, fB, fA).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, 0, -1, 0).endVertex();
        builder.vertex(mat, xMax, yMin, zMin).color(fR, fG, fB, fA).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, 0, -1, 0).endVertex();
        builder.vertex(mat, xMax, yMin, zMax).color(fR, fG, fB, fA).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, 0, -1, 0).endVertex();

        // Северная грань (-Z)
        builder.vertex(mat, xMin, yMax, zMin).color(fR, fG, fB, fA).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, 0, 0, -1).endVertex();
        builder.vertex(mat, xMax, yMax, zMin).color(fR, fG, fB, fA).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, 0, 0, -1).endVertex();
        builder.vertex(mat, xMax, yMin, zMin).color(fR, fG, fB, fA).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, 0, 0, -1).endVertex();
        builder.vertex(mat, xMin, yMin, zMin).color(fR, fG, fB, fA).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, 0, 0, -1).endVertex();

        // Южная грань (+Z)
        builder.vertex(mat, xMin, yMin, zMax).color(fR, fG, fB, fA).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, 0, 0, 1).endVertex();
        builder.vertex(mat, xMax, yMin, zMax).color(fR, fG, fB, fA).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, 0, 0, 1).endVertex();
        builder.vertex(mat, xMax, yMax, zMax).color(fR, fG, fB, fA).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, 0, 0, 1).endVertex();
        builder.vertex(mat, xMin, yMax, zMax).color(fR, fG, fB, fA).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, 0, 0, 1).endVertex();

        // Западная грань (-X)
        builder.vertex(mat, xMin, yMin, zMax).color(fR, fG, fB, fA).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, -1, 0, 0).endVertex();
        builder.vertex(mat, xMin, yMax, zMax).color(fR, fG, fB, fA).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, -1, 0, 0).endVertex();
        builder.vertex(mat, xMin, yMax, zMin).color(fR, fG, fB, fA).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, -1, 0, 0).endVertex();
        builder.vertex(mat, xMin, yMin, zMin).color(fR, fG, fB, fA).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, -1, 0, 0).endVertex();

        // Восточная грань (+X)
        builder.vertex(mat, xMax, yMin, zMin).color(fR, fG, fB, fA).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, 1, 0, 0).endVertex();
        builder.vertex(mat, xMax, yMax, zMin).color(fR, fG, fB, fA).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, 1, 0, 0).endVertex();
        builder.vertex(mat, xMax, yMax, zMax).color(fR, fG, fB, fA).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, 1, 0, 0).endVertex();
        builder.vertex(mat, xMax, yMin, zMax).color(fR, fG, fB, fA).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(norm, 1, 0, 0).endVertex();
    }

    private void drawFixedPortalBore(Matrix4f posMat, Matrix3f normMat, VertexConsumer builder, float s, float z) {
        int fullLight = 15728880;

        builder.vertex(posMat, -s, -s, z).color(0.12F, 0.02F, 0.22F, 1.0F).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        builder.vertex(posMat,  s, -s, z).color(0.12F, 0.02F, 0.22F, 1.0F).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        builder.vertex(posMat,  s,  s, z).color(0.12F, 0.02F, 0.22F, 1.0F).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        builder.vertex(posMat, -s,  s, z).color(0.12F, 0.02F, 0.22F, 1.0F).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();

        // Обратная сторона для взгляда изнутри
        builder.vertex(posMat, -s,  s, z).color(0.12F, 0.02F, 0.22F, 1.0F).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, -1.0F).endVertex();
        builder.vertex(posMat,  s,  s, z).color(0.12F, 0.02F, 0.22F, 1.0F).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, -1.0F).endVertex();
        builder.vertex(posMat,  s, -s, z).color(0.12F, 0.02F, 0.22F, 1.0F).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, -1.0F).endVertex();
        builder.vertex(posMat, -s, -s, z).color(0.12F, 0.02F, 0.22F, 1.0F).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, -1.0F).endVertex();
    }

    private void drawEndGlyph(Matrix4f posMat, Matrix3f normMat, VertexConsumer builder,
                              float s, float z, float r, float g, float b, float a, boolean isFront) {
        int fullLight = 15728880;

        if (isFront) {
            builder.vertex(posMat, -s, -s, z).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
            builder.vertex(posMat,  s, -s, z).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
            builder.vertex(posMat,  s,  s, z).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
            builder.vertex(posMat, -s,  s, z).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        } else {
            builder.vertex(posMat,  s, -s, z).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, -1.0F).endVertex();
            builder.vertex(posMat, -s, -s, z).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, -1.0F).endVertex();
            builder.vertex(posMat, -s,  s, z).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, -1.0F).endVertex();
            builder.vertex(posMat,  s,  s, z).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 0.0F, -1.0F).endVertex();
        }
    }

    private void drawWallPanel(Matrix4f posMat, Matrix3f normMat, VertexConsumer builder,
                               float hw, float halfLen, float r, float g, float b, float a) {
        int fullLight = 15728880;

        // Внешняя плоскость
        builder.vertex(posMat, -hw, 0.0F, -halfLen).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 1.0F, 0.0F).endVertex();
        builder.vertex(posMat,  hw, 0.0F, -halfLen).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 1.0F, 0.0F).endVertex();
        builder.vertex(posMat,  hw, 0.0F,  halfLen).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 1.0F, 0.0F).endVertex();
        builder.vertex(posMat, -hw, 0.0F,  halfLen).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, 1.0F, 0.0F).endVertex();

        // Внутренняя плоскость
        builder.vertex(posMat, -hw, 0.0F,  halfLen).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, -1.0F, 0.0F).endVertex();
        builder.vertex(posMat,  hw, 0.0F,  halfLen).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, -1.0F, 0.0F).endVertex();
        builder.vertex(posMat,  hw, 0.0F, -halfLen).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, -1.0F, 0.0F).endVertex();
        builder.vertex(posMat, -hw, 0.0F, -halfLen).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullLight).normal(normMat, 0.0F, -1.0F, 0.0F).endVertex();
    }
}