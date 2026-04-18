package cc.fascinated.fascinatedutils.renderer;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public class SolidColorQuadRenderState implements GuiElementRenderState {
    private final RenderPipeline pipeline;
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
    @Nullable
    private ScreenRectangle bounds;

    /**
     * @param pipeline         typically {@link FascinatedUiPipelines#SOLID_COLOR}
     * @param pose             copy of the active {@link net.minecraft.client.gui.GuiGraphicsExtractor} matrix stack head
     * @param x0               first corner x
     * @param y0               first corner y
     * @param x1               opposite corner x
     * @param y1               opposite corner y
     * @param colorTopLeft     packed ARGB top-left
     * @param colorBottomLeft  packed ARGB bottom-left
     * @param colorBottomRight packed ARGB bottom-right
     * @param colorTopRight    packed ARGB top-right
     * @param scissorArea      active scissor from the draw context
     */
    public SolidColorQuadRenderState(RenderPipeline pipeline, Matrix3x2f pose, int x0, int y0, int x1, int y1, int colorTopLeft, int colorBottomLeft, int colorBottomRight, int colorTopRight, ScreenRectangle scissorArea) {
        this.pipeline = pipeline;
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
    }

    @Nullable
    @Override
    public ScreenRectangle bounds() {
        if (bounds == null) {
            ScreenRectangle axis = RoundedRectTexRenderState.axisBounds(x0, y0, x1, y1);
            bounds = axis.transformMaxBounds(pose);
        }
        return bounds;
    }

    @Override
    public void buildVertices(VertexConsumer buffer) {
        buffer.addVertexWith2DPose(pose, x0, y0).setColor(colorTopLeft);
        buffer.addVertexWith2DPose(pose, x0, y1).setColor(colorBottomLeft);
        buffer.addVertexWith2DPose(pose, x1, y1).setColor(colorBottomRight);
        buffer.addVertexWith2DPose(pose, x1, y0).setColor(colorTopRight);
    }

    @Override
    public RenderPipeline pipeline() {
        return pipeline;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    public ScreenRectangle scissorArea() {
        return scissorArea;
    }
}
