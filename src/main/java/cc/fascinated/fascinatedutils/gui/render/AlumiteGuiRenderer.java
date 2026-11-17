package cc.fascinated.fascinatedutils.gui.render;

import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui.theme.UiThemeRepository;
import cc.fascinated.fascinatedutils.renderer.MeshBuilder;
import cc.fascinated.fascinatedutils.renderer.Renderer2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The single GUI renderer: nested clips with segment flushes, deferred text, alpha and transform stacks, click
 * regions, and a low-level {@link Renderer2D} for batched geometry and immediate chrome.
 *
 * <p>Deferred text keeps the logical {@code (x, y)} it was queued with ({@code y} is a layout line top) and is drawn
 * when the current segment flushes. Pose transforms that must apply to queued text have to still be on the stack when
 * {@link #endRenderSegment()}, {@link #flushText()} or {@link #endFrame()} runs.
 */
public class AlumiteGuiRenderer {
    private static final List<ClickRegion> FRAME_CLICK_REGIONS = new ArrayList<>();
    private static ClickRegion pressedClickRegion = null;
    private static final int TEXT_WIDTH_CACHE_LIMIT = 4096;
    private static final Map<String, Integer> plainTextWidthCache = new HashMap<>();
    private static final Map<String, Integer> boldTextWidthCache = new HashMap<>();
    private static final Map<String, Integer> guiPlainTextWidthCache = new HashMap<>();
    private static final Map<String, Integer> guiBoldTextWidthCache = new HashMap<>();
    private static Font textWidthCacheFont;

    private final GuiGraphicsExtractor drawContext;
    private final Minecraft minecraftClient;
    private final Renderer2D backend;
    private final boolean guiFont;
    private final Deque<ClipLayer> clipStack = new ArrayDeque<>();
    private final Deque<Float> alphaStack = new ArrayDeque<>();
    private final List<QueuedTextDraw> queuedTextDrawPool = new ArrayList<>();
    private int queuedTextDrawCount;
    private float logicalToVanillaScaleX = 1f;
    private float logicalToVanillaScaleY = 1f;
    private float pointerX = Float.NaN;
    private float pointerY = Float.NaN;

    public AlumiteGuiRenderer(GuiGraphicsExtractor drawContext) {
        this(drawContext, false);
    }

    public AlumiteGuiRenderer(GuiGraphicsExtractor drawContext, boolean guiFont) {
        this.drawContext = drawContext;
        this.guiFont = guiFont;
        this.minecraftClient = Minecraft.getInstance();
        this.backend = new Renderer2D(drawContext, this::currentScissorArea, guiFont);
    }

    /**
     * @return the active UI theme
     */
    public UiTheme theme() {
        return UiThemeRepository.get();
    }

    /**
     * Begins a frame: batch scope, first render segment, and a root clip covering the logical area.
     *
     * @param logicalWidth  logical width of the root clip in pixels
     * @param logicalHeight logical height of the root clip in pixels
     */
    public void beginFrame(float logicalWidth, float logicalHeight) {
        pointerX = Float.NaN;
        pointerY = Float.NaN;
        FRAME_CLICK_REGIONS.clear();
        float vanillaWidth = (float) minecraftClient.getWindow().getGuiScaledWidth();
        float vanillaHeight = (float) minecraftClient.getWindow().getGuiScaledHeight();
        logicalToVanillaScaleX = vanillaWidth / Math.max(1f, logicalWidth);
        logicalToVanillaScaleY = vanillaHeight / Math.max(1f, logicalHeight);

        drawContext.pose().pushMatrix();
        drawContext.pose().identity();
        drawContext.pose().scale(logicalToVanillaScaleX, logicalToVanillaScaleY);

        MeshBuilder.INSTANCE.beginFrame(drawContext, minecraftClient);

        ClipLayer rootLayer = new ClipLayer(0, 0, logicalWidth, logicalHeight);
        clipStack.push(rootLayer);
        alphaStack.clear();
        alphaStack.push(1f);
        backend.setMultiplyAlpha(1f);
        enableScissor(rootLayer);
    }

    /**
     * Ends the frame: flushes the last segment, pops the root clip and closes the mesh frame.
     */
    public void endFrame() {
        flushCurrentSegment();
        if (!clipStack.isEmpty()) {
            clipStack.pop();
        }
        drawContext.disableScissor();
        alphaStack.clear();
        backend.setMultiplyAlpha(1f);
        MeshBuilder.INSTANCE.endFrame(drawContext);
        drawContext.pose().popMatrix();
    }

    /**
     * Flushes batched geometry for the current segment, draws its deferred text, then reopens a mesh segment.
     */
    public void endRenderSegment() {
        flushCurrentSegment();
    }

    public void pushClip(ClipRegion clipRegion) {
        pushClip(clipRegion.positionX(), clipRegion.positionY(), clipRegion.width(), clipRegion.height());
    }

    public void pushClip(float positionX, float positionY, float width, float height) {
        ClipLayer nextLayer;
        if (!clipStack.isEmpty()) {
            nextLayer = intersect(clipStack.peek(), positionX, positionY, width, height);
            flushCurrentSegment();
            drawContext.nextStratum();
        }
        else {
            nextLayer = new ClipLayer(positionX, positionY, width, height);
        }
        clipStack.push(nextLayer);
        enableScissor(nextLayer);
    }

    public void popClip() {
        if (clipStack.isEmpty()) {
            return;
        }
        flushCurrentSegment();
        drawContext.nextStratum();
        clipStack.pop();
        drawContext.disableScissor();
    }

    public void pushAlpha(float alphaFactor) {
        float parentAlpha = alphaStack.isEmpty() ? 1f : alphaStack.peek();
        float nextAlpha = Mth.clamp(parentAlpha * alphaFactor, 0f, 1f);
        alphaStack.push(nextAlpha);
        backend.setMultiplyAlpha(nextAlpha);
    }

    public void popAlpha() {
        if (alphaStack.size() <= 1) {
            return;
        }
        alphaStack.pop();
        backend.setMultiplyAlpha(alphaStack.peek());
    }

    public void setMultiplyAlpha(float factor) {
        backend.setMultiplyAlpha(factor);
    }

    /**
     * Separate alpha multipliers for geometry and text (shell fades).
     *
     * @param geometryFactor multiplier for fills and textures
     * @param textFactor     multiplier for deferred text draws
     */
    public void setMultiplyAlpha(float geometryFactor, float textFactor) {
        backend.setMultiplyAlpha(geometryFactor, textFactor);
    }

    public void resetMultiplyAlpha() {
        backend.resetMultiplyAlpha();
    }

    public void pushTransform(float translateX, float translateY, float scaleX, float scaleY) {
        flushCurrentSegment();
        drawContext.pose().pushMatrix();
        drawContext.pose().translate(translateX, translateY);
        drawContext.pose().scale(scaleX, scaleY);
    }

    public void popTransform() {
        flushCurrentSegment();
        drawContext.pose().popMatrix();
    }

    public void pushTranslate(float offsetX, float offsetY) {
        backend.pushTranslate(offsetX, offsetY);
    }

    public void popTranslate() {
        backend.popTranslate();
    }

    public void pushScale(float scale) {
        backend.pushScale(scale);
    }

    public void popScale() {
        backend.popScale();
    }

    public void drawRect(float positionX, float positionY, float width, float height, int argbColor) {
        backend.drawRect(positionX, positionY, width, height, argbColor);
    }

    public void drawDottedOutline(float width, float height, float dotSize, float step, int argbColor) {
        backend.drawDottedOutline(width, height, dotSize, step, argbColor);
    }

    public void drawLine(float startX, float startY, float endX, float endY, float thickness, int argbColor) {
        backend.drawLine(startX, startY, endX, endY, thickness, argbColor);
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
        if (fillArgbColor != 0) {
            backend.fillRoundedRect(positionX, positionY, width, height, cornerRadius, fillArgbColor, RectCornerRoundMask.ALL);
        }
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
     * @param ringStrokePx    stroke width, scaled to framebuffer pixels using the current matrix stack
     * @param borderArgb      packed ARGB tint for the ring
     * @param cornerRoundMask which corners are rounded
     */
    public void fillRoundedRectBorderRing(float positionX, float positionY, float width, float height, float cornerRadius, float ringStrokePx, int borderArgb, int cornerRoundMask) {
        backend.fillRoundedRectBorderRing(positionX, positionY, width, height, cornerRadius, ringStrokePx, borderArgb, cornerRoundMask);
    }

    public void drawVerticalGradient(float positionX, float positionY, float width, float height, int topArgbColor, int bottomArgbColor) {
        backend.fillGradientVertical(positionX, positionY, width, height, topArgbColor, bottomArgbColor);
    }

    public void drawText(String text, float positionX, float positionY, int argbColor, boolean shadow, boolean bold) {
        nextQueuedTextDraw().setLiteral(text, positionX, positionY, backend.withQueuedTextColor(argbColor), shadow, bold);
    }

    public void drawCenteredText(String text, float centerX, float positionY, int argbColor, boolean shadow, boolean bold) {
        float left = centerX - backend.measureTextWidth(text, bold) * 0.5f;
        nextQueuedTextDraw().setLiteral(text, left, positionY, backend.withQueuedTextColor(argbColor), shadow, bold);
    }

    public void drawMiniMessageText(String miniMessageText, float positionX, float positionY, boolean shadow) {
        nextQueuedTextDraw().setVisual(backend.miniMessageVisualOrder(miniMessageText), positionX, positionY, backend.withQueuedTextColor(0xFFFFFFFF), shadow);
    }

    public int measureMiniMessageTextWidth(String miniMessageText) {
        return backend.measureMiniMessageTextWidth(miniMessageText);
    }

    /**
     * Draws a vanilla {@link Component} immediately. Call {@link #endRenderSegment()} once before the first of these
     * if batched mesh was queued in the same segment.
     *
     * @param text      rich text to draw
     * @param positionX left origin in logical pixels
     * @param positionY layout line top in logical pixels
     * @param color     packed ARGB text color
     * @param shadow    whether to draw vanilla text shadow
     */
    public void drawComponentText(Component text, float positionX, float positionY, int color, boolean shadow) {
        backend.drawTextImmediate(text, positionX, positionY, color, shadow);
    }

    /**
     * Draws all queued text now, for call sites that need text layered before later geometry.
     */
    public void flushText() {
        if (queuedTextDrawCount == 0) {
            return;
        }
        MeshBuilder.INSTANCE.endSegment(drawContext);
        drawQueuedText();
        drawContext.nextStratum();
    }

    /**
     * Drops cached plain and bold text widths so they are re-measured after a font or language reload.
     */
    public static void clearTextWidthCache() {
        plainTextWidthCache.clear();
        boldTextWidthCache.clear();
        guiPlainTextWidthCache.clear();
        guiBoldTextWidthCache.clear();
    }

    /**
     * Measures plain text width with the vanilla font. Results are cached across frames because layout re-measures the
     * same labels every frame; the cache is dropped when the font instance changes or resources reload.
     *
     * @param text the text to measure
     * @param bold whether the text is measured bold
     * @return the width in logical pixels
     */
    public int measureTextWidth(String text, boolean bold) {
        Font font = minecraftClient.font;
        if (font != textWidthCacheFont) {
            clearTextWidthCache();
            textWidthCacheFont = font;
        }
        Map<String, Integer> cache = guiFont ? (bold ? guiBoldTextWidthCache : guiPlainTextWidthCache) : (bold ? boldTextWidthCache : plainTextWidthCache);
        Integer cachedWidth = cache.get(text);
        if (cachedWidth != null) {
            return cachedWidth;
        }
        int width = backend.measureTextWidth(text, bold);
        if (cache.size() >= TEXT_WIDTH_CACHE_LIMIT) {
            cache.clear();
        }
        cache.put(text, width);
        return width;
    }

    public int measureTextWidth(Component text) {
        return backend.measureTextWidth(text);
    }

    public int fontHeight() {
        return backend.getFontHeight();
    }

    public int fontCapHeight() {
        return Math.max(1, fontHeight() - 1);
    }

    public void drawTexture(Identifier textureId, float positionX, float positionY, float width, float height, int tintArgb) {
        backend.drawTexture(textureId, positionX, positionY, width, height, tintArgb);
    }

    public void drawRoundedTexture(Identifier textureId, float positionX, float positionY, float width, float height, float cornerRadius, int tintArgb) {
        backend.drawRoundedTexture(textureId, positionX, positionY, width, height, cornerRadius, tintArgb);
    }

    public void drawSprite(Identifier spriteId, float positionX, float positionY, float width, float height, int tintArgb) {
        backend.drawSprite(spriteId, positionX, positionY, width, height, tintArgb);
    }

    public void drawSprite(Identifier spriteId, int textureWidth, int textureHeight, int sourceX, int sourceY, float positionX, float positionY, float width, float height) {
        backend.drawSprite(spriteId, textureWidth, textureHeight, sourceX, sourceY, positionX, positionY, width, height);
    }

    /**
     * Draws a GUI item stack with vanilla overlays (immediate; flushes batched geometry first).
     *
     * @param stack     item stack to draw
     * @param positionX left origin in logical pixels
     * @param positionY top origin in logical pixels
     */
    public void drawGuiItem(ItemStack stack, float positionX, float positionY) {
        backend.drawGuiItem(stack, positionX, positionY);
    }

    /**
     * Draws a GUI item for a holder entity (immediate; flushes batched geometry first).
     *
     * @param holder    living entity for glint seed
     * @param stack     item stack to draw
     * @param positionX left origin in logical pixels
     * @param positionY top origin in logical pixels
     */
    public void drawGuiItem(LivingEntity holder, ItemStack stack, float positionX, float positionY) {
        backend.drawGuiItem(holder, stack, positionX, positionY);
    }

    @Nullable
    public ClipRegion currentClip() {
        if (clipStack.isEmpty()) {
            return null;
        }
        ClipLayer top = clipStack.peek();
        ClipRegion region = top.region;
        if (region == null) {
            int left = Mth.floor(top.positionX);
            int topEdge = Mth.floor(top.positionY);
            region = new ClipRegion(left, topEdge, Mth.ceil(top.positionX + top.width) - left, Mth.ceil(top.positionY + top.height) - topEdge);
            top.region = region;
        }
        return region;
    }

    public void setPointer(float x, float y) {
        pointerX = x;
        pointerY = y;
    }

    public float pointerX() {
        return pointerX;
    }

    public float pointerY() {
        return pointerY;
    }

    public void addClickRegion(int x, int y, int w, int h, Runnable callback) {
        FRAME_CLICK_REGIONS.add(new ClickRegion(x, y, w, h, callback));
    }

    /**
     * Records whichever click region from the most recent frame is under the pointer at press
     * time, without firing its callback. Call this during {@code mouseClicked} (press).
     *
     * @param pointerX logical X of the press
     * @param pointerY logical Y of the press
     * @param button   GLFW mouse button; only button {@code 0} (primary) is checked
     *
     * @return {@code true} if a region was found under the pointer
     */
    public static boolean recordPressedRegion(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        for (ClickRegion region : FRAME_CLICK_REGIONS) {
            if (pointerX >= region.x && pointerX < region.x + region.w
                    && pointerY >= region.y && pointerY < region.y + region.h) {
                pressedClickRegion = region;
                return true;
            }
        }
        return false;
    }

    /**
     * Fires the callback of the region recorded by {@link #recordPressedRegion} if the release
     * pointer is still within that region's bounds, then clears the record. Call this during
     * {@code mouseReleased} (release).
     *
     * @param pointerX logical X of the release
     * @param pointerY logical Y of the release
     *
     * @return {@code true} if a region was fired
     */
    public static boolean fireAndClearPressedRegion(float pointerX, float pointerY) {
        ClickRegion pressed = pressedClickRegion;
        pressedClickRegion = null;
        if (pressed == null) {
            return false;
        }
        if (pointerX >= pressed.x && pointerX < pressed.x + pressed.w
                && pointerY >= pressed.y && pointerY < pressed.y + pressed.h) {
            pressed.callback().run();
            return true;
        }
        return false;
    }

    private record ClickRegion(int x, int y, int w, int h, Runnable callback) {}

    private void flushCurrentSegment() {
        MeshBuilder.INSTANCE.endSegment(drawContext);
        drawQueuedText();
    }

    private void drawQueuedText() {
        for (int index = 0; index < queuedTextDrawCount; index++) {
            QueuedTextDraw queued = queuedTextDrawPool.get(index);
            if (queued.visualText != null) {
                backend.drawTextImmediate(queued.visualText, queued.positionX, queued.positionY, queued.argbColor, queued.shadow);
            }
            else {
                backend.drawTextImmediate(queued.text, queued.positionX, queued.positionY, queued.argbColor, queued.shadow, queued.bold);
            }
            queued.release();
        }
        queuedTextDrawCount = 0;
        if (guiFont) {
            // Atlas glyphs are queued as mesh quads; submit them now so they stay in the current stratum.
            MeshBuilder.INSTANCE.endSegment(drawContext);
        }
    }

    private void enableScissor(ClipLayer clipLayer) {
        // The graphics context transforms scissor rectangles by the current pose, which already scales logical to
        // vanilla units, so the rectangle is passed in logical units. Converting it here as well would shrink every
        // clip toward the origin whenever the pose scale is not 1 (any GUI scale other than 2x the screen scale).
        int x0 = Mth.floor(clipLayer.positionX);
        int y0 = Mth.floor(clipLayer.positionY);
        int x1 = Math.max(x0, Mth.ceil(clipLayer.positionX + clipLayer.width));
        int y1 = Math.max(y0, Mth.ceil(clipLayer.positionY + clipLayer.height));
        drawContext.enableScissor(x0, y0, x1, y1);
    }

    @Nullable
    private ScreenRectangle currentScissorArea() {
        if (clipStack.isEmpty()) {
            return null;
        }
        ClipLayer clipLayer = clipStack.peek();
        ScreenRectangle scissorArea = clipLayer.scissorArea;
        if (scissorArea != null) {
            return scissorArea;
        }

        int scaledX = Mth.floor(clipLayer.positionX * logicalToVanillaScaleX);
        int scaledY = Mth.floor(clipLayer.positionY * logicalToVanillaScaleY);
        int scaledRight = Math.max(scaledX, Mth.ceil((clipLayer.positionX + clipLayer.width) * logicalToVanillaScaleX));
        int scaledBottom = Math.max(scaledY, Mth.ceil((clipLayer.positionY + clipLayer.height) * logicalToVanillaScaleY));
        scissorArea = new ScreenRectangle(new ScreenPosition(scaledX, scaledY), Math.max(0, scaledRight - scaledX), Math.max(0, scaledBottom - scaledY));
        clipLayer.scissorArea = scissorArea;
        return scissorArea;
    }

    private static ClipLayer intersect(ClipLayer parentLayer, float positionX, float positionY, float width, float height) {
        float outputX = Math.max(positionX, parentLayer.positionX);
        float outputY = Math.max(positionY, parentLayer.positionY);
        float outputRight = Math.min(positionX + width, parentLayer.positionX + parentLayer.width);
        float outputBottom = Math.min(positionY + height, parentLayer.positionY + parentLayer.height);
        return new ClipLayer(outputX, outputY, Math.max(0f, outputRight - outputX), Math.max(0f, outputBottom - outputY));
    }

    private static class ClipLayer {
        private final float positionX;
        private final float positionY;
        private final float width;
        private final float height;
        @Nullable
        private ScreenRectangle scissorArea;
        @Nullable
        private ClipRegion region;

        private ClipLayer(float positionX, float positionY, float width, float height) {
            this.positionX = positionX;
            this.positionY = positionY;
            this.width = Math.max(0f, width);
            this.height = Math.max(0f, height);
        }
    }

    private QueuedTextDraw nextQueuedTextDraw() {
        if (queuedTextDrawCount == queuedTextDrawPool.size()) {
            queuedTextDrawPool.add(new QueuedTextDraw());
        }
        return queuedTextDrawPool.get(queuedTextDrawCount++);
    }

    /**
     * A text draw waiting for the next text flush. Instances are pooled and reused across frames, so they are only
     * valid between queueing and {@link #drawQueuedText()}.
     */
    private static class QueuedTextDraw {
        private String text;
        private FormattedCharSequence visualText;
        private float positionX;
        private float positionY;
        private int argbColor;
        private boolean shadow;
        private boolean bold;

        private void setLiteral(String text, float positionX, float positionY, int argbColor, boolean shadow, boolean bold) {
            this.text = text;
            this.visualText = null;
            this.positionX = positionX;
            this.positionY = positionY;
            this.argbColor = argbColor;
            this.shadow = shadow;
            this.bold = bold;
        }

        private void setVisual(FormattedCharSequence visualText, float positionX, float positionY, int argbColor, boolean shadow) {
            this.text = null;
            this.visualText = visualText;
            this.positionX = positionX;
            this.positionY = positionY;
            this.argbColor = argbColor;
            this.shadow = shadow;
            this.bold = false;
        }

        private void release() {
            text = null;
            visualText = null;
        }
    }
}
