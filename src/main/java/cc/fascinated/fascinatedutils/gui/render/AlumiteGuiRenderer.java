package cc.fascinated.fascinatedutils.gui.render;

import cc.fascinated.fascinatedutils.gui.core.TextLineLayout;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui.theme.UiThemeRepository;
import cc.fascinated.fascinatedutils.renderer.Renderer2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * The GUI frame: owns the state that scopes drawing (the pose stack through the draw context, nested clips and the
 * alpha stack) and forwards drawing to {@link Renderer2D}.
 *
 * <p>Every draw is submitted immediately with the pose, scissor and alpha in effect at the call, and vanilla layers
 * each element above whatever it overlaps, so paint order is simply call order. Nothing is deferred, which means
 * clips, transforms and alpha scopes can be popped right after the draws they wrap.
 *
 * <p>Instances are cheap to construct and may be rebound to a new draw context with {@link #rebind}.
 */
public class AlumiteGuiRenderer {
    private static final TextLineLayout GUI_FONT_WRAPPED_LINES = new TextLineLayout();
    private static final TextLineLayout VANILLA_FONT_WRAPPED_LINES = new TextLineLayout();
    private static final int INITIAL_STACK_DEPTH = 8;
    private static final int CLIP_FLOATS = 4;
    private GuiGraphicsExtractor drawContext;
    private boolean guiFont;
    private final Minecraft minecraftClient;
    private final Renderer2D backend;
    private float[] clipStack = new float[INITIAL_STACK_DEPTH * CLIP_FLOATS];
    private ClipRegion[] clipRegionCache;
    private int clipDepth;
    private float[] alphaStack = new float[INITIAL_STACK_DEPTH];
    private int alphaDepth;
    private int poseDepth;

    /**
     * Creates a renderer for the mod screen canvas.
     *
     * @param drawContext the draw context to submit to
     * @param guiFont     whether literal text uses the mod's GUI font
     */
    public AlumiteGuiRenderer(GuiGraphicsExtractor drawContext, boolean guiFont) {
        this.drawContext = drawContext;
        this.guiFont = guiFont;
        this.minecraftClient = Minecraft.getInstance();
        this.backend = new Renderer2D(drawContext, guiFont);
    }

    /**
     * Drops cached text widths and sequences so they are re-measured after a font or language reload.
     */
    public static void clearTextWidthCache() {
        Renderer2D.clearMiniMessageWidthCache();
        GUI_FONT_WRAPPED_LINES.clearCache();
        VANILLA_FONT_WRAPPED_LINES.clearCache();
    }

    /**
     * Points this renderer at another draw context so one instance can serve many frames.
     *
     * @param drawContext the new draw context
     * @param guiFont     whether literal text uses the mod's GUI font
     */
    public void rebind(GuiGraphicsExtractor drawContext, boolean guiFont) {
        this.drawContext = drawContext;
        this.guiFont = guiFont;
        backend.rebind(drawContext, guiFont);
    }

    /**
     * @return the active UI theme
     */
    public UiTheme theme() {
        return UiThemeRepository.get();
    }

    /**
     * Begins a frame: resets every stack and the click regions, maps the logical canvas onto the vanilla GUI area and
     * opens a root clip covering it. Starts a new vanilla stratum so layering only has to compare this frame's elements.
     *
     * @param logicalWidth  logical width of the canvas in pixels
     * @param logicalHeight logical height of the canvas in pixels
     */
    public void beginFrame(float logicalWidth, float logicalHeight) {
        backend.beginFrame();
        clipDepth = 0;
        alphaDepth = 0;
        poseDepth = 0;
        float scaleX = (float) minecraftClient.getWindow().getGuiScaledWidth() / Math.max(1f, logicalWidth);
        float scaleY = (float) minecraftClient.getWindow().getGuiScaledHeight() / Math.max(1f, logicalHeight);

        drawContext.nextStratum();
        drawContext.pose().pushMatrix();
        drawContext.pose().identity();
        drawContext.pose().scale(scaleX, scaleY);

        pushAlphaLayer(1f);
        pushClipLayer(0f, 0f, logicalWidth, logicalHeight);
    }

    /**
     * Ends the frame, closing any clip, transform or alpha scope the caller left open.
     */
    public void endFrame() {
        while (clipDepth > 0) {
            clipDepth--;
            drawContext.disableScissor();
        }
        while (poseDepth > 0) {
            poseDepth--;
            drawContext.pose().popMatrix();
        }
        alphaDepth = 0;
        backend.setAlpha(1f);
        drawContext.pose().popMatrix();
    }

    /**
     * Intersects the current clip with a region.
     *
     * @param clipRegion region in logical pixels
     */
    public void pushClip(ClipRegion clipRegion) {
        pushClip(clipRegion.positionX(), clipRegion.positionY(), clipRegion.width(), clipRegion.height());
    }

    /**
     * Intersects the current clip with a rectangle. The rectangle is rounded to whole logical pixels like fills, and the
     * draw context transforms it by the current pose, so geometry and text drawn under any transform are clipped the
     * same way.
     *
     * @param positionX left edge in logical pixels
     * @param positionY top edge in logical pixels
     * @param width     width in logical pixels
     * @param height    height in logical pixels
     */
    public void pushClip(float positionX, float positionY, float width, float height) {
        if (clipDepth == 0) {
            pushClipLayer(positionX, positionY, width, height);
            return;
        }
        int parent = (clipDepth - 1) * CLIP_FLOATS;
        float left = Math.max(positionX, clipStack[parent]);
        float top = Math.max(positionY, clipStack[parent + 1]);
        float right = Math.min(positionX + width, clipStack[parent] + clipStack[parent + 2]);
        float bottom = Math.min(positionY + height, clipStack[parent + 1] + clipStack[parent + 3]);
        pushClipLayer(left, top, Math.max(0f, right - left), Math.max(0f, bottom - top));
    }

    /**
     * Removes the most recent clip. The root clip stays until {@link #endFrame}.
     */
    public void popClip() {
        if (clipDepth <= 1) {
            return;
        }
        clipDepth--;
        drawContext.disableScissor();
    }

    /**
     * Multiplies the current alpha by a factor for geometry and text alike.
     *
     * @param alphaFactor multiplier in the range 0 to 1
     */
    public void pushAlpha(float alphaFactor) {
        pushAlphaLayer(alphaStack[alphaDepth - 1] * Mth.clamp(alphaFactor, 0f, 1f));
    }

    /**
     * Restores the alpha from before the matching {@link #pushAlpha}.
     */
    public void popAlpha() {
        if (alphaDepth <= 1) {
            return;
        }
        alphaDepth--;
        applyAlpha();
    }

    /**
     * Translates then scales everything drawn until {@link #popTransform}.
     *
     * @param translateX x offset in current units
     * @param translateY y offset in current units
     * @param scaleX     horizontal scale
     * @param scaleY     vertical scale
     */
    public void pushTransform(float translateX, float translateY, float scaleX, float scaleY) {
        drawContext.pose().pushMatrix();
        drawContext.pose().translate(translateX, translateY);
        drawContext.pose().scale(scaleX, scaleY);
        poseDepth++;
    }

    /**
     * Ends the most recent {@link #pushTransform}.
     */
    public void popTransform() {
        popPose();
    }

    /**
     * Translates everything drawn until {@link #popTranslate}.
     *
     * @param offsetX x offset in current units
     * @param offsetY y offset in current units
     */
    public void pushTranslate(float offsetX, float offsetY) {
        pushTransform(offsetX, offsetY, 1f, 1f);
    }

    /**
     * Ends the most recent {@link #pushTranslate}.
     */
    public void popTranslate() {
        popPose();
    }

    public void drawRect(float positionX, float positionY, float width, float height, int argbColor) {
        backend.drawRect(positionX, positionY, width, height, argbColor);
    }

    public void drawDottedOutline(float width, float height, float dotSize, float step, int argbColor) {
        backend.drawDottedOutline(width, height, dotSize, step, argbColor);
    }

    public void drawBorder(float positionX, float positionY, float width, float height, float thickness, int argbColor) {
        backend.drawBorder(positionX, positionY, width, height, thickness, argbColor);
    }

    public void drawRoundedRect(float positionX, float positionY, float width, float height, float cornerRadius, int argbColor) {
        backend.fillRoundedRect(positionX, positionY, width, height, cornerRadius, argbColor, RectCornerRoundMask.ALL);
    }

    public void fillRoundedRect(float positionX, float positionY, float width, float height, float cornerRadius, int fillArgb, int cornerRoundMask) {
        backend.fillRoundedRect(positionX, positionY, width, height, cornerRadius, fillArgb, cornerRoundMask);
    }

    public void drawRoundedRectFrame(float positionX, float positionY, float width, float height, float cornerRadius, int borderArgbColor, int fillArgbColor, float borderThickness) {
        backend.fillRoundedRect(positionX, positionY, width, height, cornerRadius, fillArgbColor, RectCornerRoundMask.ALL);
        backend.fillRoundedRectBorderRing(positionX, positionY, width, height, cornerRadius, borderThickness, borderArgbColor, RectCornerRoundMask.ALL);
    }

    public void fillRoundedRectFrame(float positionX, float positionY, float width, float height, float cornerRadius, int borderArgb, int fillArgb, float borderThicknessX, float borderThicknessY, int cornerRoundMask) {
        backend.fillRoundedRectFrame(positionX, positionY, width, height, cornerRadius, borderArgb, fillArgb, borderThicknessX, borderThicknessY, cornerRoundMask);
    }

    /**
     * Draws only an outer border band (no inner fill), for stacking on top of a separate rounded fill pass.
     *
     * @param positionX       logical left of the outer bounds
     * @param positionY       logical top of the outer bounds
     * @param width           logical width of the outer bounds
     * @param height          logical height of the outer bounds
     * @param cornerRadius    logical corner radius in pixels
     * @param ringStroke      band thickness in logical pixels; scales with the pose like the rectangle
     * @param borderArgb      packed ARGB tint for the ring
     * @param cornerRoundMask which corners are rounded
     */
    public void fillRoundedRectBorderRing(float positionX, float positionY, float width, float height, float cornerRadius, float ringStroke, int borderArgb, int cornerRoundMask) {
        backend.fillRoundedRectBorderRing(positionX, positionY, width, height, cornerRadius, ringStroke, borderArgb, cornerRoundMask);
    }

    public void drawVerticalGradient(float positionX, float positionY, float width, float height, int topArgbColor, int bottomArgbColor) {
        backend.fillGradientVertical(positionX, positionY, width, height, topArgbColor, bottomArgbColor);
    }

    public void drawText(String text, float positionX, float positionY, int argbColor, boolean shadow, boolean bold) {
        backend.drawText(text, positionX, positionY, argbColor, shadow, bold);
    }

    public void drawMiniMessageText(String miniMessageText, float positionX, float positionY, boolean shadow) {
        backend.drawText(backend.miniMessageVisualOrder(miniMessageText), positionX, positionY, 0xFFFFFFFF, shadow);
    }

    public int measureMiniMessageTextWidth(String miniMessageText) {
        return backend.measureMiniMessageTextWidth(miniMessageText);
    }

    /**
     * Draws a vanilla {@link Component} with the current text alpha applied.
     *
     * @param text      rich text to draw
     * @param positionX left origin in logical pixels
     * @param positionY layout line top in logical pixels
     * @param color     packed ARGB text color
     * @param shadow    whether to draw vanilla text shadow
     */
    public void drawComponentText(Component text, float positionX, float positionY, int color, boolean shadow) {
        backend.drawText(text, positionX, positionY, color, shadow);
    }

    /**
     * Measures plain text width. Results are cached across frames because layout re-measures the same labels every
     * frame; the cache is dropped when the font or a font option changes, or when resources reload.
     *
     * @param text the text to measure
     * @param bold whether the text is measured bold
     * @return the width in logical pixels
     */
    public int measureTextWidth(String text, boolean bold) {
        return backend.measureTextWidth(text, bold);
    }

    public int measureTextWidth(Component text) {
        return backend.measureTextWidth(text);
    }

    /**
     * Soft-wraps literal text to a width using this renderer's font. Results are cached per font until
     * {@link #clearTextWidthCache()}.
     *
     * @param text     the text to wrap
     * @param maxWidth the available width in logical pixels
     * @param bold     whether the text is bold
     * @param maxLines the most lines to return; the last kept line gets an ellipsis when text was cut
     * @return the wrapped lines, unmodifiable
     */
    public List<String> wrapLines(String text, float maxWidth, boolean bold, int maxLines) {
        TextLineLayout layout = guiFont ? GUI_FONT_WRAPPED_LINES : VANILLA_FONT_WRAPPED_LINES;
        return layout.wrapLines(text, maxWidth, bold, maxLines, line -> backend.measureTextWidth(line, bold));
    }

    public int fontHeight() {
        return backend.getFontHeight();
    }

    public void drawTexture(Identifier textureId, float positionX, float positionY, float width, float height, int tintArgb) {
        backend.drawTexture(textureId, positionX, positionY, width, height, tintArgb);
    }

    public void drawRoundedTexture(Identifier textureId, float positionX, float positionY, float width, float height, float cornerRadius, int tintArgb, float textureU0, float textureV0, float textureU1, float textureV1) {
        backend.drawRoundedTexture(textureId, positionX, positionY, width, height, cornerRadius, tintArgb, textureU0, textureV0, textureU1, textureV1);
    }

    public void drawSprite(Identifier spriteId, float positionX, float positionY, float width, float height, int tintArgb) {
        backend.drawSprite(spriteId, positionX, positionY, width, height, tintArgb);
    }

    public void drawSprite(Identifier spriteId, int textureWidth, int textureHeight, int sourceX, int sourceY, float positionX, float positionY, float width, float height) {
        backend.drawSprite(spriteId, textureWidth, textureHeight, sourceX, sourceY, positionX, positionY, width, height);
    }

    /**
     * Draws a GUI item stack with vanilla overlays.
     *
     * @param stack     item stack to draw
     * @param positionX left origin in logical pixels
     * @param positionY top origin in logical pixels
     */
    public void drawGuiItem(ItemStack stack, float positionX, float positionY) {
        backend.drawGuiItem(stack, positionX, positionY);
    }

    /**
     * Draws a GUI item for a holder entity.
     *
     * @param holder    living entity for glint seed
     * @param stack     item stack to draw
     * @param positionX left origin in logical pixels
     * @param positionY top origin in logical pixels
     */
    public void drawGuiItem(LivingEntity holder, ItemStack stack, float positionX, float positionY) {
        backend.drawGuiItem(holder, stack, positionX, positionY);
    }

    /**
     * The current clip in whole logical pixels, for hit testing.
     *
     * @return the innermost clip, or null outside a frame
     */
    @Nullable
    public ClipRegion currentClip() {
        if (clipDepth == 0) {
            return null;
        }
        if (clipRegionCache == null || clipRegionCache.length < clipDepth) {
            clipRegionCache = new ClipRegion[clipStack.length / CLIP_FLOATS];
        }
        ClipRegion region = clipRegionCache[clipDepth - 1];
        if (region == null) {
            int base = (clipDepth - 1) * CLIP_FLOATS;
            int left = Math.round(clipStack[base]);
            int top = Math.round(clipStack[base + 1]);
            region = new ClipRegion(left, top, Math.round(clipStack[base] + clipStack[base + 2]) - left, Math.round(clipStack[base + 1] + clipStack[base + 3]) - top);
            clipRegionCache[clipDepth - 1] = region;
        }
        return region;
    }

    private void popPose() {
        if (poseDepth > 0) {
            poseDepth--;
            drawContext.pose().popMatrix();
        }
    }

    private void pushClipLayer(float positionX, float positionY, float width, float height) {
        int base = clipDepth * CLIP_FLOATS;
        if (base + CLIP_FLOATS > clipStack.length) {
            clipStack = Arrays.copyOf(clipStack, clipStack.length * 2);
            clipRegionCache = null;
        }
        clipStack[base] = positionX;
        clipStack[base + 1] = positionY;
        clipStack[base + 2] = Math.max(0f, width);
        clipStack[base + 3] = Math.max(0f, height);
        if (clipRegionCache != null && clipDepth < clipRegionCache.length) {
            clipRegionCache[clipDepth] = null;
        }
        clipDepth++;
        // The draw context transforms the rectangle by the current pose and intersects it with the enclosing scissor,
        // so it is given in current units; both geometry and text read that one scissor.
        int left = Math.round(positionX);
        int top = Math.round(positionY);
        drawContext.enableScissor(left, top, Math.max(left, Math.round(positionX + width)), Math.max(top, Math.round(positionY + height)));
    }

    private void pushAlphaLayer(float alphaFactor) {
        if (alphaDepth == alphaStack.length) {
            alphaStack = Arrays.copyOf(alphaStack, alphaStack.length * 2);
        }
        alphaStack[alphaDepth++] = alphaFactor;
        applyAlpha();
    }

    private void applyAlpha() {
        backend.setAlpha(alphaStack[alphaDepth - 1]);
    }
}
