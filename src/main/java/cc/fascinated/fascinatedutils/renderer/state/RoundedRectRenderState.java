package cc.fascinated.fascinatedutils.renderer.state;

import cc.fascinated.fascinatedutils.renderer.pipeline.FascinatedUiPipelines;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/**
 * A rounded rectangle, filled or as an outer ring, whose shape parameters are written into the vertex data laid out by
 * {@link FascinatedUiPipelines#ROUNDED_RECT_FORMAT}. All instances share one pipeline and texture setup so they batch.
 */
public class RoundedRectRenderState implements GuiElementRenderState {
    private static final float HALF_SIZE_STEPS_PER_UNIT = 8f;
    private static final float STROKE_STEPS_PER_UNIT = 64f;
    private static final float TEXCOORD_STEPS_PER_UNIT = 16384f;
    private final RenderPipeline pipeline;
    private final TextureSetup textureSetup;
    private final Matrix3x2fc pose;
    private final float left;
    private final float top;
    private final float right;
    private final float bottom;
    private final int colorTop;
    private final int colorBottom;
    private final float cornerRadius;
    private final int cornerMask;
    private final float ringStroke;
    private final float textureU0;
    private final float textureV0;
    private final float textureU1;
    private final float textureV1;
    @Nullable
    private final ScreenRectangle scissorArea;
    @Nullable
    private final ScreenRectangle bounds;

    /**
     * Creates a rounded rectangle.
     *
     * @param pipeline     {@link FascinatedUiPipelines#ROUNDED_RECT} or {@link FascinatedUiPipelines#ROUNDED_RECT_TEXTURED}
     * @param textureSetup texture setup matching the pipeline
     * @param pose         immutable pose copy; shared between elements, never mutated
     * @param left         local left edge of the shape
     * @param top          local top edge of the shape
     * @param right        local right edge of the shape
     * @param bottom       local bottom edge of the shape
     * @param colorTop     packed ARGB at the top vertices
     * @param colorBottom  packed ARGB at the bottom vertices
     * @param cornerRadius corner radius in local units
     * @param cornerMask   which corners are rounded, see {@code RectCornerRoundMask}
     * @param ringStroke   ring thickness in local units, or 0 for a solid fill
     * @param scissorArea  active scissor in screen space, or null
     */
    public RoundedRectRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose, float left, float top, float right, float bottom, int colorTop, int colorBottom, float cornerRadius, int cornerMask, float ringStroke, @Nullable ScreenRectangle scissorArea) {
        this(pipeline, textureSetup, pose, left, top, right, bottom, colorTop, colorBottom, cornerRadius, cornerMask, ringStroke, 0f, 0f, 1f, 1f, scissorArea);
    }

    /**
     * Creates a rounded rectangle that samples a sub-rectangle of its texture. The corner mask and ring stroke are
     * ignored by {@link FascinatedUiPipelines#ROUNDED_RECT_TEXTURED}, which always fills with all corners rounded.
     *
     * @param pipeline     {@link FascinatedUiPipelines#ROUNDED_RECT} or {@link FascinatedUiPipelines#ROUNDED_RECT_TEXTURED}
     * @param textureSetup texture setup matching the pipeline
     * @param pose         immutable pose copy; shared between elements, never mutated
     * @param left         local left edge of the shape
     * @param top          local top edge of the shape
     * @param right        local right edge of the shape
     * @param bottom       local bottom edge of the shape
     * @param colorTop     packed ARGB at the top vertices
     * @param colorBottom  packed ARGB at the bottom vertices
     * @param cornerRadius corner radius in local units
     * @param cornerMask   which corners are rounded, see {@code RectCornerRoundMask}
     * @param ringStroke   ring thickness in local units, or 0 for a solid fill
     * @param textureU0    texture u at the left edge, 0 to 1 across the full texture
     * @param textureV0    texture v at the top edge
     * @param textureU1    texture u at the right edge
     * @param textureV1    texture v at the bottom edge
     * @param scissorArea  active scissor in screen space, or null
     */
    public RoundedRectRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose, float left, float top, float right, float bottom, int colorTop, int colorBottom, float cornerRadius, int cornerMask, float ringStroke, float textureU0, float textureV0, float textureU1, float textureV1, @Nullable ScreenRectangle scissorArea) {
        this.pipeline = pipeline;
        this.textureSetup = textureSetup;
        this.pose = pose;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.colorTop = colorTop;
        this.colorBottom = colorBottom;
        this.cornerRadius = cornerRadius;
        this.cornerMask = cornerMask;
        this.ringStroke = ringStroke;
        this.textureU0 = textureU0;
        this.textureV0 = textureV0;
        this.textureU1 = textureU1;
        this.textureV1 = textureV1;
        this.scissorArea = scissorArea;
        this.bounds = GuiBounds.of(pose, left, top, right, bottom, scissorArea);
    }

    @Nullable
    @Override
    public ScreenRectangle bounds() {
        return bounds;
    }

    @Override
    public void buildVertices(VertexConsumer buffer) {
        float halfWidth = (right - left) * 0.5f;
        float halfHeight = (bottom - top) * 0.5f;
        int packedHalfWidth = Mth.clamp(Math.round(halfWidth * HALF_SIZE_STEPS_PER_UNIT), 0, Short.MAX_VALUE);
        int packedHalfHeight = Mth.clamp(Math.round(halfHeight * HALF_SIZE_STEPS_PER_UNIT), 0, Short.MAX_VALUE);
        int packedStroke = Mth.clamp(Math.round(ringStroke * STROKE_STEPS_PER_UNIT), 0, Short.MAX_VALUE);
        addVertex(buffer, left, top, -halfWidth, -halfHeight, colorTop, packedHalfWidth, packedHalfHeight, packedStroke, textureU0, textureV0);
        addVertex(buffer, left, bottom, -halfWidth, halfHeight, colorBottom, packedHalfWidth, packedHalfHeight, packedStroke, textureU0, textureV1);
        addVertex(buffer, right, bottom, halfWidth, halfHeight, colorBottom, packedHalfWidth, packedHalfHeight, packedStroke, textureU1, textureV1);
        addVertex(buffer, right, top, halfWidth, -halfHeight, colorTop, packedHalfWidth, packedHalfHeight, packedStroke, textureU1, textureV0);
    }

    private void addVertex(VertexConsumer buffer, float localX, float localY, float offsetX, float offsetY, int color, int packedHalfWidth, int packedHalfHeight, int packedStroke, float textureU, float textureV) {
        boolean textured = pipeline == FascinatedUiPipelines.ROUNDED_RECT_TEXTURED;
        float screenX = pose.m00() * localX + pose.m10() * localY + pose.m20();
        float screenY = pose.m01() * localX + pose.m11() * localY + pose.m21();
        buffer.addVertex(screenX, screenY, cornerRadius).setUv(offsetX, offsetY).setColor(color).setUv1(packedHalfWidth, packedHalfHeight).setUv2(textured ? packedTexCoord(textureU) : cornerMask, textured ? packedTexCoord(textureV) : packedStroke);
    }

    private static int packedTexCoord(float coordinate) {
        return Mth.clamp(Math.round(coordinate * TEXCOORD_STEPS_PER_UNIT), Short.MIN_VALUE, Short.MAX_VALUE);
    }

    @Override
    public RenderPipeline pipeline() {
        return pipeline;
    }

    @Override
    public TextureSetup textureSetup() {
        return textureSetup;
    }

    @Nullable
    @Override
    public ScreenRectangle scissorArea() {
        return scissorArea;
    }
}
