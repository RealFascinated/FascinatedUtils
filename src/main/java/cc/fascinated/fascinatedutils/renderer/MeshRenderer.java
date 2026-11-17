package cc.fascinated.fascinatedutils.renderer;

import cc.fascinated.fascinatedutils.renderer.pipeline.FascinatedUiPipelines;
import cc.fascinated.fascinatedutils.renderer.state.RoundedRectRenderState;
import cc.fascinated.fascinatedutils.renderer.state.QuadRenderState;
import cc.fascinated.fascinatedutils.renderer.state.GlyphRunRenderState;
import cc.fascinated.fascinatedutils.renderer.state.FrameRenderState;
import cc.fascinated.fascinatedutils.renderer.state.DottedOutlineRenderState;
import cc.fascinated.fascinatedutils.gui.render.RectCornerRoundMask;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

/**
 * Submits GUI geometry straight into the active {@code GuiRenderState}, one element per call, with the pose and scissor
 * captured at call time. Vanilla layers each element above whatever it overlaps, so submitting in paint order is all
 * that is needed for correct ordering, and it sorts and merges elements that share a pipeline, scissor and texture
 * setup into single draws.
 *
 * <p>Rectangles are snapped by rounding both edges, so a one unit line stays one unit and abutting rectangles never
 * overlap or leave a gap.
 */
public class MeshRenderer {
    private GuiGraphicsExtractor graphics;
    private Matrix3x2f sharedPose;

    /**
     * Creates a renderer bound to the given draw context.
     *
     * @param graphics the draw context elements are submitted to
     */
    public MeshRenderer(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }

    /**
     * Texture setup that samples the given texture with linear filtering and edge clamping.
     *
     * @param texture the texture to sample
     * @return a setup equal to any other setup built for the same texture
     */
    public static TextureSetup linearTextureSetup(AbstractTexture texture) {
        return TextureSetup.singleTexture(texture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
    }

    /**
     * Points the renderer at a different draw context and forgets the cached pose.
     *
     * @param graphics the new draw context
     */
    public void rebind(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
        this.sharedPose = null;
    }

    /**
     * The scissor in screen space that every element submitted right now is clipped to.
     *
     * @return the active scissor, or null when none is set
     */
    public ScreenRectangle scissor() {
        return graphics.scissorStack.peek();
    }

    /**
     * An immutable copy of the current pose. Consecutive calls with an unchanged pose return the same instance.
     *
     * @return the shared pose copy; never mutate it
     */
    public Matrix3x2fc pose() {
        Matrix3x2f live = graphics.pose();
        Matrix3x2f cached = sharedPose;
        if (cached == null || cached.m00() != live.m00() || cached.m01() != live.m01() || cached.m10() != live.m10() || cached.m11() != live.m11() || cached.m20() != live.m20() || cached.m21() != live.m21()) {
            cached = new Matrix3x2f(live);
            sharedPose = cached;
        }
        return cached;
    }

    /**
     * Submits an already built element.
     *
     * @param element the element to add at the current paint position
     */
    public void submit(GuiElementRenderState element) {
        graphics.guiRenderState.addGuiElement(element);
    }

    /**
     * Fills a rectangle with per-corner colors.
     *
     * @param positionX       local left
     * @param positionY       local top
     * @param width           local width
     * @param height          local height
     * @param colorTopLeft    packed ARGB
     * @param colorBottomLeft packed ARGB
     * @param colorBottomRight packed ARGB
     * @param colorTopRight   packed ARGB
     */
    public void fillRect(float positionX, float positionY, float width, float height, int colorTopLeft, int colorBottomLeft, int colorBottomRight, int colorTopRight) {
        int left = Math.round(positionX);
        int top = Math.round(positionY);
        int right = Math.max(left + 1, Math.round(positionX + width));
        int bottom = Math.max(top + 1, Math.round(positionY + height));
        submit(new QuadRenderState(RenderPipelines.GUI, TextureSetup.noTexture(), pose(), left, top, right, bottom, 0f, 0f, 1f, 1f, colorTopLeft, colorBottomLeft, colorBottomRight, colorTopRight, scissor()));
    }

    /**
     * Draws a square-cornered border inside the given rectangle.
     *
     * @param positionX local left
     * @param positionY local top
     * @param width     local width
     * @param height    local height
     * @param thickness requested thickness in local units; rounded, at least one
     * @param color     packed ARGB
     */
    public void border(float positionX, float positionY, float width, float height, float thickness, int color) {
        int left = Math.round(positionX);
        int top = Math.round(positionY);
        int right = Math.max(left + 1, Math.round(positionX + width));
        int bottom = Math.max(top + 1, Math.round(positionY + height));
        submit(new FrameRenderState(pose(), left, top, right, bottom, Math.max(1, Math.round(thickness)), color, scissor()));
    }

    /**
     * Draws a dotted rectangle outline from the local origin.
     *
     * @param width   outline width in local units
     * @param height  outline height in local units
     * @param dotSize dot side length in local units
     * @param step    distance between dot starts in local units
     * @param color   packed ARGB
     */
    public void dottedOutline(float width, float height, float dotSize, float step, int color) {
        submit(new DottedOutlineRenderState(pose(), Math.round(width), Math.round(height), Math.round(dotSize), Math.round(step), color, scissor()));
    }

    /**
     * Draws a rounded rectangle, filled or as an outer ring.
     *
     * @param positionX    local left
     * @param positionY    local top
     * @param width        local width
     * @param height       local height
     * @param cornerRadius corner radius in local units
     * @param colorTop     packed ARGB at the top
     * @param colorBottom  packed ARGB at the bottom
     * @param cornerMask   which corners are rounded
     * @param ringStroke   ring thickness in local units, or 0 for a solid fill
     */
    public void roundedRect(float positionX, float positionY, float width, float height, float cornerRadius, int colorTop, int colorBottom, int cornerMask, float ringStroke) {
        if (cornerMask == RectCornerRoundMask.NONE || cornerRadius <= 0f) {
            if (ringStroke > 0f) {
                border(positionX, positionY, width, height, ringStroke, colorTop);
            }
            else {
                fillRect(positionX, positionY, width, height, colorTop, colorBottom, colorBottom, colorTop);
            }
            return;
        }
        Snapped bounds = snapShape(positionX, positionY, width, height);
        submit(new RoundedRectRenderState(FascinatedUiPipelines.ROUNDED_RECT, TextureSetup.noTexture(), pose(), bounds.left(), bounds.top(), bounds.right(), bounds.bottom(), colorTop, colorBottom, cornerRadius, cornerMask, ringStroke, scissor()));
    }

    /**
     * Draws a texture over a rectangle.
     *
     * @param textureSetup linearly filtered texture setup
     * @param positionX    local left
     * @param positionY    local top
     * @param width        local width
     * @param height       local height
     * @param tint         packed ARGB tint
     */
    public void texturedRect(TextureSetup textureSetup, float positionX, float positionY, float width, float height, int tint) {
        int left = Math.round(positionX);
        int top = Math.round(positionY);
        int right = Math.max(left + 1, Math.round(positionX + width));
        int bottom = Math.max(top + 1, Math.round(positionY + height));
        submit(new QuadRenderState(RenderPipelines.GUI_TEXTURED, textureSetup, pose(), left, top, right, bottom, 0f, 0f, 1f, 1f, tint, tint, tint, tint, scissor()));
    }

    /**
     * Draws a sub-rectangle of a texture over a rounded rectangle with all corners rounded.
     *
     * @param textureSetup linearly filtered texture setup
     * @param positionX    local left
     * @param positionY    local top
     * @param width        local width
     * @param height       local height
     * @param cornerRadius corner radius in local units
     * @param tint         packed ARGB tint
     * @param textureU0    texture u at the left edge
     * @param textureV0    texture v at the top edge
     * @param textureU1    texture u at the right edge
     * @param textureV1    texture v at the bottom edge
     */
    public void roundedTexturedRect(TextureSetup textureSetup, float positionX, float positionY, float width, float height, float cornerRadius, int tint, float textureU0, float textureV0, float textureU1, float textureV1) {
        Snapped bounds = snapShape(positionX, positionY, width, height);
        submit(new RoundedRectRenderState(FascinatedUiPipelines.ROUNDED_RECT_TEXTURED, textureSetup, pose(), bounds.left(), bounds.top(), bounds.right(), bounds.bottom(), tint, tint, cornerRadius, RectCornerRoundMask.ALL, 0f, textureU0, textureV0, textureU1, textureV1, scissor()));
    }

    /**
     * Submits a run of glyph quads as one element.
     *
     * @param textureSetup atlas texture setup
     * @param glyphs       packed glyph quads, see {@link GlyphRunRenderState}
     * @param glyphCount   number of glyphs
     * @param color        packed ARGB shared by all glyphs
     */
    public void glyphRun(TextureSetup textureSetup, float[] glyphs, int glyphCount, int color) {
        submit(new GlyphRunRenderState(textureSetup, pose(), glyphs, glyphCount, color, scissor()));
    }

    /**
     * Snaps a rounded shape to whole local units. Square shapes keep a fixed rounded side centred on the original
     * rectangle so circles do not change size as they animate across fractional positions.
     */
    private static Snapped snapShape(float positionX, float positionY, float width, float height) {
        int left = Math.round(positionX);
        int top = Math.round(positionY);
        int right = Math.round(positionX + width);
        int bottom = Math.round(positionY + height);
        if (Math.abs(width - height) < 1e-3f) {
            int side = Math.max(1, Math.round(width));
            left = Math.round(positionX + width * 0.5f - side * 0.5f);
            top = Math.round(positionY + height * 0.5f - side * 0.5f);
            right = left + side;
            bottom = top + side;
        }
        return new Snapped(left, top, Math.max(left + 1, right), Math.max(top + 1, bottom));
    }

    private record Snapped(int left, int top, int right, int bottom) {}
}
