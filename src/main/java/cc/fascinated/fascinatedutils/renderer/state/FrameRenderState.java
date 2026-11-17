package cc.fascinated.fascinatedutils.renderer.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/**
 * A square-cornered rectangular border drawn as up to four non-overlapping quads in one element, so translucent borders
 * never double-blend at the corners.
 */
public class FrameRenderState implements GuiElementRenderState {
    private final Matrix3x2fc pose;
    private final int left;
    private final int top;
    private final int right;
    private final int bottom;
    private final int thickness;
    private final int color;
    @Nullable
    private final ScreenRectangle scissorArea;
    @Nullable
    private final ScreenRectangle bounds;

    /**
     * Creates a border.
     *
     * @param pose        immutable pose copy; shared between elements, never mutated
     * @param left        local left edge of the outer rectangle
     * @param top         local top edge of the outer rectangle
     * @param right       local right edge of the outer rectangle
     * @param bottom      local bottom edge of the outer rectangle
     * @param thickness   border thickness in local units, at least one
     * @param color       packed ARGB color
     * @param scissorArea active scissor in screen space, or null
     */
    public FrameRenderState(Matrix3x2fc pose, int left, int top, int right, int bottom, int thickness, int color, @Nullable ScreenRectangle scissorArea) {
        this.pose = pose;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.thickness = thickness;
        this.color = color;
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
        if (right - left <= 2 * thickness || bottom - top <= 2 * thickness) {
            addQuad(buffer, left, top, right, bottom);
            return;
        }
        addQuad(buffer, left, top, right, top + thickness);
        addQuad(buffer, left, bottom - thickness, right, bottom);
        addQuad(buffer, left, top + thickness, left + thickness, bottom - thickness);
        addQuad(buffer, right - thickness, top + thickness, right, bottom - thickness);
    }

    private void addQuad(VertexConsumer buffer, float quadLeft, float quadTop, float quadRight, float quadBottom) {
        buffer.addVertexWith2DPose(pose, quadLeft, quadTop).setColor(color);
        buffer.addVertexWith2DPose(pose, quadLeft, quadBottom).setColor(color);
        buffer.addVertexWith2DPose(pose, quadRight, quadBottom).setColor(color);
        buffer.addVertexWith2DPose(pose, quadRight, quadTop).setColor(color);
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Nullable
    @Override
    public ScreenRectangle scissorArea() {
        return scissorArea;
    }
}
