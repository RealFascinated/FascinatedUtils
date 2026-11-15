package cc.fascinated.fascinatedutils.renderer;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.Mth;
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
     * @param pose                 copy of the active {@link GuiGraphicsExtractor} matrix stack head
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

    public static ScreenRectangle clippedBounds(int x0, int y0, int x1, int y1, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
        float minX = Math.min(x0, x1);
        float minY = Math.min(y0, y1);
        float maxX = Math.max(x0, x1);
        float maxY = Math.max(y0, y1);

        float transformedMinX;
        float transformedMaxX;
        float transformedMinY;
        float transformedMaxY;
        if (pose.m01() == 0f && pose.m10() == 0f) {
            // GUI poses are translations and scales, so each axis maps on its own and two corners cover the quad.
            float left = pose.m00() * minX + pose.m20();
            float right = pose.m00() * maxX + pose.m20();
            float top = pose.m11() * minY + pose.m21();
            float bottom = pose.m11() * maxY + pose.m21();
            transformedMinX = Math.min(left, right);
            transformedMaxX = Math.max(left, right);
            transformedMinY = Math.min(top, bottom);
            transformedMaxY = Math.max(top, bottom);
        }
        else {
            float cornerX0 = pose.m00() * minX + pose.m10() * minY + pose.m20();
            float cornerY0 = pose.m01() * minX + pose.m11() * minY + pose.m21();
            float cornerX1 = pose.m00() * maxX + pose.m10() * minY + pose.m20();
            float cornerY1 = pose.m01() * maxX + pose.m11() * minY + pose.m21();
            float cornerX2 = pose.m00() * minX + pose.m10() * maxY + pose.m20();
            float cornerY2 = pose.m01() * minX + pose.m11() * maxY + pose.m21();
            float cornerX3 = pose.m00() * maxX + pose.m10() * maxY + pose.m20();
            float cornerY3 = pose.m01() * maxX + pose.m11() * maxY + pose.m21();
            transformedMinX = Math.min(Math.min(cornerX0, cornerX2), Math.min(cornerX1, cornerX3));
            transformedMaxX = Math.max(Math.max(cornerX0, cornerX2), Math.max(cornerX1, cornerX3));
            transformedMinY = Math.min(Math.min(cornerY0, cornerY2), Math.min(cornerY1, cornerY3));
            transformedMaxY = Math.max(Math.max(cornerY0, cornerY2), Math.max(cornerY1, cornerY3));
        }

        int left = Mth.floor(transformedMinX);
        int top = Mth.floor(transformedMinY);
        int right = left + Mth.ceil(transformedMaxX - transformedMinX);
        int bottom = top + Mth.ceil(transformedMaxY - transformedMinY);
        if (scissorArea != null) {
            left = Math.max(left, scissorArea.left());
            top = Math.max(top, scissorArea.top());
            right = Math.min(right, scissorArea.right());
            bottom = Math.min(bottom, scissorArea.bottom());
            if (left >= right || top >= bottom) {
                return ScreenRectangle.empty();
            }
        }
        return new ScreenRectangle(left, top, right - left, bottom - top);
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
            bounds = clippedBounds(x0, y0, x1, y1, pose, scissorArea);
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
