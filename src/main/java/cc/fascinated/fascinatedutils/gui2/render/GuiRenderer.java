package cc.fascinated.fascinatedutils.gui2.render;

import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import cc.fascinated.fascinatedutils.oldgui.GuiTheme;
import cc.fascinated.fascinatedutils.oldgui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.renderer.MeshBuilder;
import cc.fascinated.fascinatedutils.renderer.Renderer2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class GuiRenderer implements RenderFrame {
    private static final List<ClickRegion> FRAME_CLICK_REGIONS = new ArrayList<>();
    private static ClickRegion pressedClickRegion = null;

    private final GuiGraphicsExtractor drawContext;
    private final Minecraft minecraftClient;
    private final UiTheme uiTheme;
    private final Renderer2D backend;
    private final Deque<ClipLayer> clipStack = new ArrayDeque<>();
    private final Deque<Float> alphaStack = new ArrayDeque<>();
    private final List<QueuedTextDraw> queuedTextDraws = new ArrayList<>();
    private float logicalToVanillaScaleX = 1f;
    private float logicalToVanillaScaleY = 1f;
    private float pointerX = Float.NaN;
    private float pointerY = Float.NaN;

    public GuiRenderer(GuiGraphicsExtractor drawContext, GuiTheme guiTheme, UiTheme uiTheme) {
        this.drawContext = drawContext;
        this.minecraftClient = Minecraft.getInstance();
        this.uiTheme = uiTheme;
        this.backend = new Renderer2D(drawContext, guiTheme, this::currentScissorArea);
    }

    @Override
    public UiTheme theme() {
        return uiTheme;
    }

    @Override
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
        MeshBuilder.INSTANCE.beginSegment(drawContext);

        ClipLayer rootLayer = new ClipLayer(0, 0, Math.round(logicalWidth), Math.round(logicalHeight));
        clipStack.push(rootLayer);
        alphaStack.clear();
        alphaStack.push(1f);
        backend.setMultiplyAlpha(1f);
        enableScissor(rootLayer);
    }

    @Override
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

    @Override
    public void pushClip(ClipRegion clipRegion) {
        ClipLayer nextLayer;
        if (!clipStack.isEmpty()) {
            ClipLayer parentLayer = clipStack.peek();
            nextLayer = intersect(parentLayer, clipRegion.positionX(), clipRegion.positionY(), clipRegion.width(), clipRegion.height());
            flushCurrentSegment();
            drawContext.nextStratum();
        }
        else {
            nextLayer = new ClipLayer(clipRegion.positionX(), clipRegion.positionY(), clipRegion.width(), clipRegion.height());
        }
        clipStack.push(nextLayer);
        enableScissor(nextLayer);
        MeshBuilder.INSTANCE.beginSegment(drawContext);
    }

    @Override
    public void popClip() {
        if (clipStack.isEmpty()) {
            return;
        }
        flushCurrentSegment();
        drawContext.nextStratum();
        clipStack.pop();
        drawContext.disableScissor();
        MeshBuilder.INSTANCE.beginSegment(drawContext);
    }

    @Override
    public void pushAlpha(float alphaFactor) {
        float parentAlpha = alphaStack.isEmpty() ? 1f : alphaStack.peek();
        float nextAlpha = Mth.clamp(parentAlpha * alphaFactor, 0f, 1f);
        alphaStack.push(nextAlpha);
        backend.setMultiplyAlpha(nextAlpha);
    }

    @Override
    public void popAlpha() {
        if (alphaStack.size() <= 1) {
            return;
        }
        alphaStack.pop();
        backend.setMultiplyAlpha(alphaStack.peek());
    }

    @Override
    public void pushTransform(float translateX, float translateY, float scaleX, float scaleY) {
        flushCurrentSegment();
        drawContext.pose().pushMatrix();
        drawContext.pose().translate(translateX, translateY);
        drawContext.pose().scale(scaleX, scaleY);
    }

    @Override
    public void popTransform() {
        flushCurrentSegment();
        drawContext.pose().popMatrix();
    }

    @Override
    public void drawRect(int positionX, int positionY, int width, int height, int argbColor) {
        backend.drawRect(positionX, positionY, width, height, argbColor);
    }

    @Override
    public void drawLine(int startX, int startY, int endX, int endY, float thickness, int argbColor) {
        backend.drawLine(startX, startY, endX, endY, thickness, argbColor);
    }

    @Override
    public void drawBorder(int positionX, int positionY, int width, int height, int thickness, int argbColor) {
        backend.drawBorder(positionX, positionY, width, height, thickness, argbColor);
    }

    @Override
    public void drawRoundedRect(int positionX, int positionY, int width, int height, int cornerRadius, int argbColor) {
        backend.fillRoundedRect(positionX, positionY, width, height, cornerRadius, argbColor, RectCornerRoundMask.ALL);
    }

    @Override
    public void drawRoundedRectFrame(int positionX, int positionY, int width, int height, int cornerRadius, int borderArgbColor, int fillArgbColor, int borderThickness) {
        if (fillArgbColor != 0) {
            backend.fillRoundedRect(positionX, positionY, width, height, cornerRadius, fillArgbColor, RectCornerRoundMask.ALL);
        }
        backend.fillRoundedRectBorderRing(positionX, positionY, width, height, cornerRadius, borderThickness, borderArgbColor, RectCornerRoundMask.ALL);
    }

    @Override
    public void drawVerticalGradient(int positionX, int positionY, int width, int height, int topArgbColor, int bottomArgbColor) {
        backend.fillGradientVertical(positionX, positionY, width, height, topArgbColor, bottomArgbColor);
    }

    @Override
    public void drawText(String text, int positionX, int positionY, int argbColor, boolean shadow, boolean bold) {
        queuedTextDraws.add(new QueuedTextDraw(text, positionX, positionY, backend.withQueuedTextColor(argbColor), shadow, bold));
    }

    @Override
    public void flushText() {
        if (queuedTextDraws.isEmpty()) {
            return;
        }
        MeshBuilder.INSTANCE.endSegment(drawContext);
        for (QueuedTextDraw queuedTextDraw : queuedTextDraws) {
            backend.drawTextImmediate(queuedTextDraw.text(), queuedTextDraw.positionX(), queuedTextDraw.positionY(), queuedTextDraw.argbColor(), queuedTextDraw.shadow(), queuedTextDraw.bold());
        }
        queuedTextDraws.clear();
        drawContext.nextStratum();
        MeshBuilder.INSTANCE.beginSegment(drawContext);
    }

    @Override
    public int measureTextWidth(String text, boolean bold) {
        return backend.measureTextWidth(text, bold);
    }

    @Override
    public int fontHeight() {
        return backend.getFontHeight();
    }

    @Override
    public void drawTexture(Identifier textureId, int positionX, int positionY, int width, int height, int tintArgb) {
        backend.drawTexture(textureId, positionX, positionY, width, height, tintArgb);
    }

    @Override
    public void drawRoundedTexture(Identifier textureId, int positionX, int positionY, int width, int height, int cornerRadius, int tintArgb) {
        backend.drawRoundedTexture(textureId, positionX, positionY, width, height, cornerRadius, tintArgb);
    }

    @Override
    public ClipRegion currentClip() {
        if (clipStack.isEmpty()) {
            return null;
        }
        ClipLayer top = clipStack.peek();
        return new ClipRegion(top.positionX, top.positionY, top.width, top.height);
    }

    public void setPointer(float x, float y) {
        pointerX = x;
        pointerY = y;
    }

    @Override
    public float pointerX() {
        return pointerX;
    }

    @Override
    public float pointerY() {
        return pointerY;
    }

    @Override
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
        for (QueuedTextDraw queuedTextDraw : queuedTextDraws) {
            backend.drawTextImmediate(queuedTextDraw.text(), queuedTextDraw.positionX(), queuedTextDraw.positionY(), queuedTextDraw.argbColor(), queuedTextDraw.shadow(), queuedTextDraw.bold());
        }
        queuedTextDraws.clear();
        MeshBuilder.INSTANCE.beginSegment(drawContext);
    }

    private void enableScissor(ClipLayer clipLayer) {
        int x0 = Mth.floor(clipLayer.positionX * logicalToVanillaScaleX);
        int y0 = Mth.floor(clipLayer.positionY * logicalToVanillaScaleY);
        int x1 = Math.max(x0, Mth.ceil((clipLayer.positionX + clipLayer.width) * logicalToVanillaScaleX));
        int y1 = Math.max(y0, Mth.ceil((clipLayer.positionY + clipLayer.height) * logicalToVanillaScaleY));
        drawContext.enableScissor(x0, y0, x1, y1);
    }

    @Nullable
    private ScreenRectangle currentScissorArea() {
        if (clipStack.isEmpty()) {
            return null;
        }
        ClipLayer clipLayer = clipStack.peek();
        int scaledX = Mth.floor(clipLayer.positionX * logicalToVanillaScaleX);
        int scaledY = Mth.floor(clipLayer.positionY * logicalToVanillaScaleY);
        int scaledRight = Math.max(scaledX, Mth.ceil((clipLayer.positionX + clipLayer.width) * logicalToVanillaScaleX));
        int scaledBottom = Math.max(scaledY, Mth.ceil((clipLayer.positionY + clipLayer.height) * logicalToVanillaScaleY));
        return new ScreenRectangle(new ScreenPosition(scaledX, scaledY), Math.max(0, scaledRight - scaledX), Math.max(0, scaledBottom - scaledY));
    }

    private static ClipLayer intersect(ClipLayer parentLayer, int positionX, int positionY, int width, int height) {
        int outputX = Math.max(positionX, parentLayer.positionX);
        int outputY = Math.max(positionY, parentLayer.positionY);
        int outputRight = Math.min(positionX + width, parentLayer.positionX + parentLayer.width);
        int outputBottom = Math.min(positionY + height, parentLayer.positionY + parentLayer.height);
        return new ClipLayer(outputX, outputY, Math.max(0, outputRight - outputX), Math.max(0, outputBottom - outputY));
    }

    private static class ClipLayer {
        private final int positionX;
        private final int positionY;
        private final int width;
        private final int height;

        private ClipLayer(int positionX, int positionY, int width, int height) {
            this.positionX = positionX;
            this.positionY = positionY;
            this.width = Math.max(0, width);
            this.height = Math.max(0, height);
        }
    }

    private record QueuedTextDraw(String text, int positionX, int positionY, int argbColor, boolean shadow, boolean bold) {
    }
}
