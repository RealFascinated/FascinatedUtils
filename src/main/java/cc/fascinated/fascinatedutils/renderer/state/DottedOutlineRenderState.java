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
 * A dotted rectangle outline as one GUI element that emits every dot quad, instead of one element per dot.
 *
 * <p>The GUI render state checks each added element against the others in its layer, so hundreds of single-dot
 * elements per outline made adding them cost grow with the square of the dot count.
 */
public class DottedOutlineRenderState implements GuiElementRenderState {
    private final Matrix3x2fc pose;
    private final int width;
    private final int height;
    private final int dotSize;
    private final int step;
    private final int color;
    @Nullable
    private final ScreenRectangle scissorArea;
    @Nullable
    private final ScreenRectangle bounds;

    /**
     * Creates the outline state.
     *
     * @param pose        immutable pose copy; the outline spans from the local origin
     * @param width       outline width in local units
     * @param height      outline height in local units
     * @param dotSize     side length of each dot in local units, at least one
     * @param step        distance between the starts of neighbouring dots in local units, at least one
     * @param color       packed ARGB dot color
     * @param scissorArea active scissor in screen space, or null
     */
    public DottedOutlineRenderState(Matrix3x2fc pose, int width, int height, int dotSize, int step, int color, @Nullable ScreenRectangle scissorArea) {
        this.pose = pose;
        this.width = width;
        this.height = height;
        this.dotSize = Math.max(1, dotSize);
        this.step = Math.max(1, step);
        this.color = color;
        this.scissorArea = scissorArea;
        this.bounds = GuiBounds.of(pose, 0f, 0f, width, height, scissorArea);
    }

    @Nullable
    @Override
    public ScreenRectangle bounds() {
        return bounds;
    }

    @Override
    public void buildVertices(VertexConsumer buffer) {
        int maxX = Math.max(0, width - dotSize);
        int maxY = Math.max(0, height - dotSize);
        for (int dotX = 0; dotX <= maxX; dotX += step) {
            addDot(buffer, dotX, 0);
            addDot(buffer, dotX, maxY);
        }
        for (int dotY = step; dotY < maxY; dotY += step) {
            addDot(buffer, 0, dotY);
            addDot(buffer, maxX, dotY);
        }
    }

    private void addDot(VertexConsumer buffer, int dotX, int dotY) {
        int dotRight = dotX + dotSize;
        int dotBottom = dotY + dotSize;
        buffer.addVertexWith2DPose(pose, dotX, dotY).setColor(color);
        buffer.addVertexWith2DPose(pose, dotX, dotBottom).setColor(color);
        buffer.addVertexWith2DPose(pose, dotRight, dotBottom).setColor(color);
        buffer.addVertexWith2DPose(pose, dotRight, dotY).setColor(color);
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
