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
 * A run of text glyph quads sharing one atlas texture and color, submitted as a single element so layering cost does
 * not grow with the glyph count. Quads are stored as {@code x0, y0, x1, y1, u0, v0, u1, v1} floats.
 */
public class GlyphRunRenderState implements GuiElementRenderState {
    public static final int FLOATS_PER_GLYPH = 8;
    private final TextureSetup textureSetup;
    private final Matrix3x2fc pose;
    private final float[] glyphs;
    private final int glyphCount;
    private final int color;
    @Nullable
    private final ScreenRectangle scissorArea;
    @Nullable
    private final ScreenRectangle bounds;

    /**
     * Creates a glyph run.
     *
     * @param textureSetup linearly filtered atlas texture setup
     * @param pose         immutable pose copy; shared between elements, never mutated
     * @param glyphs       packed glyph quads; ownership passes to the state
     * @param glyphCount   number of glyphs in {@code glyphs}
     * @param color        packed ARGB color of every glyph
     * @param scissorArea  active scissor in screen space, or null
     */
    public GlyphRunRenderState(TextureSetup textureSetup, Matrix3x2fc pose, float[] glyphs, int glyphCount, int color, @Nullable ScreenRectangle scissorArea) {
        this.textureSetup = textureSetup;
        this.pose = pose;
        this.glyphs = glyphs;
        this.glyphCount = glyphCount;
        this.color = color;
        this.scissorArea = scissorArea;
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int glyph = 0; glyph < glyphCount; glyph++) {
            int base = glyph * FLOATS_PER_GLYPH;
            minX = Math.min(minX, glyphs[base]);
            minY = Math.min(minY, glyphs[base + 1]);
            maxX = Math.max(maxX, glyphs[base + 2]);
            maxY = Math.max(maxY, glyphs[base + 3]);
        }
        this.bounds = glyphCount == 0 ? null : GuiBounds.of(pose, minX, minY, maxX, maxY, scissorArea);
    }

    @Nullable
    @Override
    public ScreenRectangle bounds() {
        return bounds;
    }

    @Override
    public void buildVertices(VertexConsumer buffer) {
        for (int glyph = 0; glyph < glyphCount; glyph++) {
            int base = glyph * FLOATS_PER_GLYPH;
            float left = glyphs[base];
            float top = glyphs[base + 1];
            float right = glyphs[base + 2];
            float bottom = glyphs[base + 3];
            float uvLeft = glyphs[base + 4];
            float uvTop = glyphs[base + 5];
            float uvRight = glyphs[base + 6];
            float uvBottom = glyphs[base + 7];
            buffer.addVertexWith2DPose(pose, left, top).setUv(uvLeft, uvTop).setColor(color);
            buffer.addVertexWith2DPose(pose, left, bottom).setUv(uvLeft, uvBottom).setColor(color);
            buffer.addVertexWith2DPose(pose, right, bottom).setUv(uvRight, uvBottom).setColor(color);
            buffer.addVertexWith2DPose(pose, right, top).setUv(uvRight, uvTop).setColor(color);
        }
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI_TEXTURED;
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
