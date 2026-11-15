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
 * A dotted rectangle outline as one GUI element that emits every dot quad, instead of one element per dot.
 *
 * <p>The GUI render state checks each added element against the others in its layer, so hundreds of single-dot
 * elements per outline made adding them cost grow with the square of the dot count.
 */
public class DottedOutlineRenderState implements GuiElementRenderState {
    private final RenderPipeline pipeline;
    private final TextureSetup textureSetup;
    private final Matrix3x2f pose;
    private final float width;
    private final float height;
    private final float dotSize;
    private final float step;
    private final int color;
    private final ScreenRectangle scissorArea;
    @Nullable
    private ScreenRectangle bounds;

    /**
     * Creates the outline state.
     *
     * @param pipeline     typically {@link FascinatedUiPipelines#AXIS_TEX_COLOR}
     * @param textureSetup opaque white texture setup
     * @param pose         copy of the active pose; the outline spans from the local origin
     * @param width        outline width in local units
     * @param height       outline height in local units
     * @param dotSize      side length of each dot
     * @param step         distance between the starts of neighbouring dots
     * @param color        packed ARGB dot color
     * @param scissorArea  active scissor from the draw context
     */
    public DottedOutlineRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose, float width, float height, float dotSize, float step, int color, ScreenRectangle scissorArea) {
        this.pipeline = pipeline;
        this.textureSetup = textureSetup;
        this.pose = pose;
        this.width = width;
        this.height = height;
        this.dotSize = dotSize;
        this.step = step;
        this.color = color;
        this.scissorArea = scissorArea;
    }

    @Nullable
    @Override
    public ScreenRectangle bounds() {
        if (bounds == null) {
            bounds = RoundedRectTexRenderState.clippedBounds(0, 0, Mth.ceil(width), Mth.ceil(height), pose, scissorArea);
        }
        return bounds;
    }

    @Override
    public void buildVertices(VertexConsumer buffer) {
        float maxX = Math.max(0f, width - dotSize);
        float maxY = Math.max(0f, height - dotSize);
        for (float dotX = 0f; dotX <= maxX; dotX += step) {
            addDot(buffer, dotX, 0f);
            addDot(buffer, dotX, maxY);
        }
        for (float dotY = step; dotY < maxY; dotY += step) {
            addDot(buffer, 0f, dotY);
            addDot(buffer, maxX, dotY);
        }
    }

    private void addDot(VertexConsumer buffer, float positionX, float positionY) {
        int x0 = Mth.floor(positionX);
        int y0 = Mth.floor(positionY);
        int x1 = Mth.ceil(positionX + dotSize);
        int y1 = Mth.ceil(positionY + dotSize);
        buffer.addVertexWith2DPose(pose, x0, y0).setUv(0f, 0f).setColor(color);
        buffer.addVertexWith2DPose(pose, x0, y1).setUv(0f, 1f).setColor(color);
        buffer.addVertexWith2DPose(pose, x1, y1).setUv(1f, 1f).setColor(color);
        buffer.addVertexWith2DPose(pose, x1, y0).setUv(1f, 0f).setColor(color);
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
