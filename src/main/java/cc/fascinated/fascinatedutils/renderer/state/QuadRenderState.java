package cc.fascinated.fascinatedutils.renderer.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/**
 * One axis-aligned (in local space) quad with float coordinates, per-corner colors and an optional texture rectangle.
 * It serves solid fills and gradients on {@code GUI}, and textured quads and rotated lines on {@code GUI_TEXTURED}; a
 * pipeline without a UV element ignores the texture coordinates.
 */
public class QuadRenderState implements GuiElementRenderState {
    private final RenderPipeline pipeline;
    private final TextureSetup textureSetup;
    private final Matrix3x2fc pose;
    private final float left;
    private final float top;
    private final float right;
    private final float bottom;
    private final float uvLeft;
    private final float uvTop;
    private final float uvRight;
    private final float uvBottom;
    private final int colorTopLeft;
    private final int colorBottomLeft;
    private final int colorBottomRight;
    private final int colorTopRight;
    @Nullable
    private final ScreenRectangle scissorArea;
    @Nullable
    private final ScreenRectangle bounds;

    /**
     * Creates a quad.
     *
     * @param pipeline         pipeline to draw with
     * @param textureSetup     texture setup, {@link TextureSetup#noTexture()} for solid quads
     * @param pose             immutable pose copy; shared between quads, never mutated
     * @param left             local left edge
     * @param top              local top edge
     * @param right            local right edge
     * @param bottom           local bottom edge
     * @param uvLeft           texture u at the left edge
     * @param uvTop            texture v at the top edge
     * @param uvRight          texture u at the right edge
     * @param uvBottom         texture v at the bottom edge
     * @param colorTopLeft     packed ARGB at the top left vertex
     * @param colorBottomLeft  packed ARGB at the bottom left vertex
     * @param colorBottomRight packed ARGB at the bottom right vertex
     * @param colorTopRight    packed ARGB at the top right vertex
     * @param scissorArea      active scissor in screen space, or null
     */
    public QuadRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose, float left, float top, float right, float bottom, float uvLeft, float uvTop, float uvRight, float uvBottom, int colorTopLeft, int colorBottomLeft, int colorBottomRight, int colorTopRight, @Nullable ScreenRectangle scissorArea) {
        this.pipeline = pipeline;
        this.textureSetup = textureSetup;
        this.pose = pose;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.uvLeft = uvLeft;
        this.uvTop = uvTop;
        this.uvRight = uvRight;
        this.uvBottom = uvBottom;
        this.colorTopLeft = colorTopLeft;
        this.colorBottomLeft = colorBottomLeft;
        this.colorBottomRight = colorBottomRight;
        this.colorTopRight = colorTopRight;
        this.scissorArea = scissorArea;
        this.bounds = GuiBounds.of(pose, Math.min(left, right), Math.min(top, bottom), Math.max(left, right), Math.max(top, bottom), scissorArea);
    }

    @Nullable
    @Override
    public ScreenRectangle bounds() {
        return bounds;
    }

    @Override
    public void buildVertices(VertexConsumer buffer) {
        buffer.addVertexWith2DPose(pose, left, top).setUv(uvLeft, uvTop).setColor(colorTopLeft);
        buffer.addVertexWith2DPose(pose, left, bottom).setUv(uvLeft, uvBottom).setColor(colorBottomLeft);
        buffer.addVertexWith2DPose(pose, right, bottom).setUv(uvRight, uvBottom).setColor(colorBottomRight);
        buffer.addVertexWith2DPose(pose, right, top).setUv(uvRight, uvTop).setColor(colorTopRight);
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
