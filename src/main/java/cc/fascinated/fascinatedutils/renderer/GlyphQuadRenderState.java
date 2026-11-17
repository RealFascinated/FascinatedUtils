package cc.fascinated.fascinatedutils.renderer;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/**
 * One text glyph quad sampling a sub-rectangle of a glyph atlas, positioned with sub-pixel float coordinates.
 */
public class GlyphQuadRenderState implements GuiElementRenderState {
    private final TextureSetup textureSetup;
    private final Matrix3x2f pose;
    private final float x0;
    private final float y0;
    private final float x1;
    private final float y1;
    private final float u0;
    private final float v0;
    private final float u1;
    private final float v1;
    private final int color;
    private final ScreenRectangle scissorArea;
    @Nullable
    private ScreenRectangle bounds;

    public GlyphQuadRenderState(TextureSetup textureSetup, Matrix3x2f pose, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, int color, ScreenRectangle scissorArea) {
        this.textureSetup = textureSetup;
        this.pose = pose;
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
        this.color = color;
        this.scissorArea = scissorArea;
    }

    @Nullable
    @Override
    public ScreenRectangle bounds() {
        if (bounds == null) {
            bounds = RoundedRectTexRenderState.clippedBounds(Mth.floor(x0), Mth.floor(y0), Mth.ceil(x1), Mth.ceil(y1), pose, scissorArea);
        }
        return bounds;
    }

    @Override
    public void buildVertices(VertexConsumer buffer) {
        buffer.addVertexWith2DPose(pose, x0, y0).setUv(u0, v0).setColor(color);
        buffer.addVertexWith2DPose(pose, x0, y1).setUv(u0, v1).setColor(color);
        buffer.addVertexWith2DPose(pose, x1, y1).setUv(u1, v1).setColor(color);
        buffer.addVertexWith2DPose(pose, x1, y0).setUv(u1, v0).setColor(color);
    }

    @Override
    public RenderPipeline pipeline() {
        return FascinatedUiPipelines.AXIS_TEX_COLOR;
    }

    @Override
    public TextureSetup textureSetup() {
        return textureSetup;
    }

    @Override
    public ScreenRectangle scissorArea() {
        return scissorArea;
    }
}
