package cc.fascinated.fascinatedutils.renderer;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public class RoundedRectTexRenderState implements GuiElementRenderState {
    private final RenderPipeline pipeline;
    private final TextureSetup textureSetup;
    private final Matrix3x2f pose;
    private final int x0;
    private final int y0;
    private final int x1;
    private final int y1;
    private final int colorTopLeft;
    private final int colorBottomLeft;
    private final int colorBottomRight;
    private final int colorTopRight;
    private final ScreenRectangle scissorArea;
    /**
     * Packed corner radius bytes for {@link FascinatedUiPipelines#ROUNDED_RECT_TEX_LUT}; unused (0) for preset
     * pipelines.
     */
    private final int packedCornerRadiiLut;
    /**
     * Outer ring stroke in logical pixels for {@code fui_rounded_rect}; 0 for solid fill.
     */
    private final float lutOuterRingStrokePx;
    @Nullable
    private ScreenRectangle bounds;

    /**
     * @param pipeline             rounded pipeline (preset or {@link FascinatedUiPipelines#ROUNDED_RECT_TEX_LUT})
     * @param textureSetup         opaque white texture setup
     * @param pose                 copy of the active {@link net.minecraft.client.gui.GuiGraphicsExtractor} matrix stack head
     * @param x0                   first corner x (logical)
     * @param y0                   first corner y
     * @param x1                   opposite corner x
     * @param y1                   opposite corner y
     * @param colorTopLeft         packed ARGB top-left tint
     * @param colorBottomLeft      bottom-left vertex color
     * @param colorBottomRight     bottom-right vertex color
     * @param colorTopRight        top-right vertex color
     * @param scissorArea          active scissor from the draw context
     * @param packedCornerRadiiLut packed radii for LUT path; 0 for preset path
     * @param lutOuterRingStrokePx ring stroke width for LUT path; 0 for solid fill
     */
    public RoundedRectTexRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose, int x0, int y0, int x1, int y1, int colorTopLeft, int colorBottomLeft, int colorBottomRight, int colorTopRight, ScreenRectangle scissorArea, int packedCornerRadiiLut, float lutOuterRingStrokePx) {
        this.pipeline = pipeline;
        this.textureSetup = textureSetup;
        this.pose = pose;
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.colorTopLeft = colorTopLeft;
        this.colorBottomLeft = colorBottomLeft;
        this.colorBottomRight = colorBottomRight;
        this.colorTopRight = colorTopRight;
        this.scissorArea = scissorArea;
        this.packedCornerRadiiLut = packedCornerRadiiLut;
        this.lutOuterRingStrokePx = lutOuterRingStrokePx;
    }

    /**
     * Build axis-aligned bounds for this quad in logical coordinates (no matrix transform).
     *
     * @param x0 first corner x
     * @param y0 first corner y
     * @param x1 opposite corner x
     * @param y1 opposite corner y
     * @return bounding {@link ScreenRectangle} for GUI render sorting
     */
    public static ScreenRectangle axisBounds(int x0, int y0, int x1, int y1) {
        int minX = Math.min(x0, x1);
        int minY = Math.min(y0, y1);
        int maxX = Math.max(x0, x1);
        int maxY = Math.max(y0, y1);
        return new ScreenRectangle(new ScreenPosition(minX, minY), maxX - minX, maxY - minY);
    }

    public int packedCornerRadiiLut() {
        return packedCornerRadiiLut;
    }

    public float lutOuterRingStrokePx() {
        return lutOuterRingStrokePx;
    }

    @Nullable
    @Override
    public ScreenRectangle bounds() {
        if (bounds == null) {
            ScreenRectangle axis = axisBounds(x0, y0, x1, y1);
            bounds = axis.transformMaxBounds(pose);
        }
        return bounds;
    }

    @Override
    public void buildVertices(VertexConsumer buffer) {
        buffer.addVertexWith2DPose(pose, x0, y0).setUv(0f, 0f).setColor(colorTopLeft);
        buffer.addVertexWith2DPose(pose, x0, y1).setUv(0f, 1f).setColor(colorBottomLeft);
        buffer.addVertexWith2DPose(pose, x1, y1).setUv(1f, 1f).setColor(colorBottomRight);
        buffer.addVertexWith2DPose(pose, x1, y0).setUv(1f, 0f).setColor(colorTopRight);
    }

    @Override
    public RenderPipeline pipeline() {
        return pipeline;
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
