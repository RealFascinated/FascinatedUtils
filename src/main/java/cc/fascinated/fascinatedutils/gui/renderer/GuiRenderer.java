package cc.fascinated.fascinatedutils.gui.renderer;

import cc.fascinated.fascinatedutils.gui.GuiDesignSpace;
import cc.fascinated.fascinatedutils.gui.GuiTheme;
import cc.fascinated.fascinatedutils.gui.renderer.operations.GuiRenderOperation;
import cc.fascinated.fascinatedutils.gui.renderer.operations.MiniMessageTextOperation;
import cc.fascinated.fascinatedutils.gui.renderer.operations.TextOperation;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.theme.UiColor;
import cc.fascinated.fascinatedutils.renderer.MeshBuilder;
import cc.fascinated.fascinatedutils.renderer.Renderer2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * GUI orchestrator: root {@link GuiGraphics} scope, nested scissors with segment flushes, deferred text
 * operations, and a single low-level {@link Renderer2D} for batched geometry and immediate chrome.
 *
 * <p>Deferred text stores the same logical {@code (x, y)} passed to {@link #drawText} / {@link #drawMiniMessageText} as
 * TextOperation (no pre-bake through the matrix stack); {@code y} is a layout line top and is
 * converted to vanilla {@link GuiGraphics#drawString} baseline coordinates in {@link Renderer2D}. {@link GuiGraphics}
 * transforms that must
 * apply to text must still be on the stack when {@link #endRenderSegment()} or {@link #end()} runs—call
 * {@link #endRenderSegment()} before popping translate/scale that were active while queueing (see HUD paths), and end
 * the frame only after any shell matrix stack work needed for deferred text is still applied (see mod settings shell).
 */
public class GuiRenderer implements UIRenderer {
    private static final int TEXT_OPERATION_POOL_CAP = 256;

    private final GuiGraphicsExtractor drawContext;
    private final Minecraft minecraftClient;
    private final GuiTheme guiTheme;
    private final Renderer2D backend;
    private final Deque<Scissor.Region> scissorStack = new ArrayDeque<>();
    private final List<GuiRenderOperation<?>> textQueue = new ArrayList<>();
    private final List<Runnable> absolutePostTasks = new ArrayList<>();
    private final ArrayDeque<TextOperation> textOperationPool = new ArrayDeque<>();
    private final ArrayDeque<MiniMessageTextOperation> miniMessageOperationPool = new ArrayDeque<>();

    public GuiRenderer(GuiGraphicsExtractor drawContext, GuiTheme guiTheme) {
        this.drawContext = drawContext;
        this.minecraftClient = Minecraft.getInstance();
        this.guiTheme = guiTheme;
        this.backend = new Renderer2D(drawContext, guiTheme);
    }

    @Override
    public GuiTheme theme() {
        return guiTheme;
    }

    /**
     * Begin the GUI frame: batch scope, first render segment, and a root scissor covering the logical clip rectangle.
     *
     * @param clipWidth  logical width of the root clip in pixels
     * @param clipHeight logical height of the root clip in pixels
     */
    public void begin(float clipWidth, float clipHeight) {
        MeshBuilder.INSTANCE.beginFrame(drawContext, minecraftClient);
        MeshBuilder.INSTANCE.beginSegment(drawContext);
        Scissor.Region root = new Scissor.Region(0f, 0f, clipWidth, clipHeight);
        scissorStack.push(root);
        drawContext.enableScissor(root.ix0, root.iy0, root.ix1, root.iy1);
    }

    /**
     * Flush batched geometry for the current scissor segment, execute deferred text for that segment, then reopen a
     * mesh segment (endRender).
     */
    public void endRenderSegment() {
        MeshBuilder.INSTANCE.endSegment(drawContext);
        for (GuiRenderOperation<?> operation : textQueue) {
            operation.execute(backend);
            recycleTextOperation(operation);
        }
        textQueue.clear();
        MeshBuilder.INSTANCE.beginSegment(drawContext);
    }

    /**
     * Queue work after the current scissor closes (post).
     *
     * @param task runnable executed after {@link #popClip()} for the active scissor
     */
    public void post(Runnable task) {
        if (scissorStack.isEmpty()) {
            throw new IllegalStateException("post() called with empty scissor stack");
        }
        scissorStack.peek().postTasks.add(task);
    }

    /**
     * Queue work after the outer {@link #end()} completes (Meteor {@code GuiRenderer#absolutePost}).
     *
     * @param task runnable executed at end of frame after root scissor teardown
     */
    public void absolutePost(Runnable task) {
        absolutePostTasks.add(task);
    }

    @Override
    public void pushClip(float positionX, float positionY, float width, float height) {
        pushClipWithLogicalOutset(positionX, positionY, width, height, 0f, 0f);
    }

    @Override
    public void pushClipWithLogicalOutset(float positionX, float positionY, float width, float height, float horizontalOutset, float verticalOutset) {
        float clipX = positionX - horizontalOutset;
        float clipY = positionY - verticalOutset;
        float clipW = width + 2f * horizontalOutset;
        float clipH = height + 2f * verticalOutset;
        Scissor.Region region;
        if (!scissorStack.isEmpty()) {
            Scissor.Region parent = scissorStack.peek();
            region = Scissor.intersect(parent, clipX, clipY, clipW, clipH);
            endRenderSegment();
        }
        else {
            region = new Scissor.Region(clipX, clipY, Math.max(0f, clipW), Math.max(0f, clipH));
        }
        scissorStack.push(region);
        drawContext.enableScissor(region.ix0, region.iy0, region.ix1, region.iy1);
        MeshBuilder.INSTANCE.beginSegment(drawContext);
    }

    @Override
    public void popClip() {
        endRenderSegment();
        Scissor.Region closed = scissorStack.pop();
        for (Runnable task : closed.postTasks) {
            task.run();
        }
        closed.postTasks.clear();
        drawContext.disableScissor();
        if (!scissorStack.isEmpty()) {
            Scissor.Region parent = scissorStack.peek();
            drawContext.enableScissor(parent.ix0, parent.iy0, parent.ix1, parent.iy1);
        }
        MeshBuilder.INSTANCE.beginSegment(drawContext);
    }

    /**
     * End the GUI frame: flush the last segment, pop the root scissor, run absolute posts, and close the mesh frame.
     */
    public void end() {
        endRenderSegment();
        if (!scissorStack.isEmpty()) {
            Scissor.Region root = scissorStack.pop();
            for (Runnable task : root.postTasks) {
                task.run();
            }
            root.postTasks.clear();
        }
        drawContext.disableScissor();
        for (Runnable task : absolutePostTasks) {
            task.run();
        }
        absolutePostTasks.clear();
        MeshBuilder.INSTANCE.endFrame(drawContext);
    }

    @Override
    public void drawRect(float positionX, float positionY, float width, float height, int color) {
        backend.drawRect(positionX, positionY, width, height, color);
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2, float thickness, int color) {
        backend.drawLine(x1, y1, x2, y2, thickness, color);
    }

    @Override
    public void fillGradientVertical(float positionX, float positionY, float width, float height, int colorTop, int colorBottom) {
        backend.fillGradientVertical(positionX, positionY, width, height, colorTop, colorBottom);
    }

    @Override
    public void fillRoundedGradientVertical(float positionX, float positionY, float width, float height, float cornerRadius, int colorTop, int colorBottom, int cornerRoundMask) {
        backend.fillRoundedGradientVertical(positionX, positionY, width, height, cornerRadius, colorTop, colorBottom, cornerRoundMask);
    }

    /**
     * Axis-aligned outline using the same LUT rounded-rect ring shader as {@link #fillRoundedRectBorderRing} with zero
     * corner radius (batched mesh, not {@link net.minecraft.client.gui.GuiGraphicsExtractor#fill}).
     */
    @Override
    public void drawBorder(float positionX, float positionY, float width, float height, float thickness, int color) {
        backend.drawBorder(positionX, positionY, width, height, thickness, color);
    }

    @Override
    public void fillRoundedRect(float positionX, float positionY, float width, float height, float cornerRadius, int fillArgb, int cornerRoundMask) {
        backend.fillRoundedRect(positionX, positionY, width, height, cornerRadius, fillArgb, cornerRoundMask);
    }

    @Override
    public void fillRoundedRectFrame(float positionX, float positionY, float width, float height, float cornerRadius, int borderArgb, int fillArgb, float borderThicknessX, float borderThicknessY, int cornerRoundMask) {
        backend.fillRoundedRectFrame(positionX, positionY, width, height, cornerRadius, borderArgb, fillArgb, borderThicknessX, borderThicknessY, cornerRoundMask);
    }

    /**
     * Draws only an outer border band on the LUT rounded-rectangle shader (no inner fill), for stacking on top of a
     * separate {@link #fillRoundedRect} pass without darkening the panel interior.
     *
     * @param positionX       logical left of the outer bounds
     * @param positionY       logical top of the outer bounds
     * @param width           logical width of the outer bounds
     * @param height          logical height of the outer bounds
     * @param cornerRadius    logical corner radius in pixels
     * @param ringStrokePx    same units as {@link #drawBorder} thickness for this rect; scaled to framebuffer pixels using
     *                        the current matrix stack so it matches {@link #drawBorder} under GUI and nested scales
     * @param borderArgb      packed ARGB tint for the ring
     * @param cornerRoundMask which corners are rounded
     */
    public void fillRoundedRectBorderRing(float positionX, float positionY, float width, float height, float cornerRadius, float ringStrokePx, int borderArgb, int cornerRoundMask) {
        backend.fillRoundedRectBorderRing(positionX, positionY, width, height, cornerRadius, ringStrokePx, borderArgb, cornerRoundMask);
    }

    /**
     * Filled rounded panel using {@link GuiTheme#border()} around {@link GuiTheme#surface()} with a uniform stroke from
     * {@link UITheme#BORDER_THICKNESS_PX}. Used for visibility cards and similar; the mod settings shell uses
     * {@link ModSettingsTheme#SHELL_BORDER} explicitly.
     *
     * @param positionX       logical left of the outer bounds
     * @param positionY       logical top of the outer bounds
     * @param width           logical width of the outer bounds
     * @param height          logical height of the outer bounds
     * @param cornerRadius    logical corner radius in pixels
     * @param cornerRoundMask which corners are rounded
     */
    public void fillThemedSurfaceCardFrame(float positionX, float positionY, float width, float height, float cornerRadius, int cornerRoundMask) {
        float strokePx = GuiDesignSpace.pxUniform(UITheme.BORDER_THICKNESS_PX);
        fillRoundedRectFrame(positionX, positionY, width, height, cornerRadius, theme().border(), theme().surface(), strokePx, strokePx, cornerRoundMask);
    }

    @Override
    public void drawText(String text, float positionX, float positionY, int color, boolean shadow, boolean bold) {
        TextOperation operation = borrowTextOperation();
        operation.set(positionX, positionY, backend.withQueuedTextColor(color));
        operation.set(text, shadow, bold);
        queueText(operation);
    }

    @Override
    public void drawCenteredText(String text, float centerX, float positionY, int color, boolean shadow, boolean bold) {
        int textWidth = backend.measureTextWidth(text, bold);
        float leftLocal = centerX - textWidth * 0.5f;
        TextOperation operation = borrowTextOperation();
        operation.set(leftLocal, positionY, backend.withQueuedTextColor(color));
        operation.set(text, shadow, bold);
        queueText(operation);
    }

    @Override
    public void drawMiniMessageText(String miniMessageText, float positionX, float positionY, boolean shadow) {
        MiniMessageTextOperation operation = borrowMiniMessageOperation();
        operation.set(positionX, positionY, backend.withQueuedTextColor(UiColor.argb("#ffffff")));
        operation.set(backend.parseMiniMessage(miniMessageText), shadow);
        queueText(operation);
    }

    @Override
    public int measureMiniMessageTextWidth(String miniMessageText) {
        return backend.measureTextWidth(backend.parseMiniMessage(miniMessageText));
    }

    @Override
    public void drawTexture(Identifier texture, float positionX, float positionY, float width, float height, int tintArgb) {
        backend.drawTexture(texture, positionX, positionY, width, height, tintArgb);
    }

    public void drawSprite(Identifier spriteId, float positionX, float positionY, float width, float height, int tintArgb) {
        backend.drawSprite(spriteId, positionX, positionY, width, height, tintArgb);
    }

    @Override
    public void pushTranslate(float offsetX, float offsetY) {
        backend.pushTranslate(offsetX, offsetY);
    }

    @Override
    public void popTranslate() {
        backend.popTranslate();
    }

    @Override
    public void pushScale(float scale) {
        backend.pushScale(scale);
    }

    @Override
    public void popScale() {
        backend.popScale();
    }

    @Override
    public int measureTextWidth(String text, boolean bold) {
        return backend.measureTextWidth(text, bold);
    }

    @Override
    public int getFontHeight() {
        return backend.getFontHeight();
    }

    @Override
    public void setMultiplyAlpha(float factor) {
        backend.setMultiplyAlpha(factor);
    }

    @Override
    public void resetMultiplyAlpha() {
        backend.resetMultiplyAlpha();
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

    /**
     * Draw a GUI item stack with vanilla overlays (immediate; flushes batched geometry first).
     *
     * @param stack     item stack to draw
     * @param positionX left origin in logical pixels
     * @param positionY top origin in logical pixels
     */
    public void drawGuiItem(ItemStack stack, float positionX, float positionY) {
        backend.drawGuiItem(stack, positionX, positionY);
    }

    /**
     * Draw a GUI item for a holder entity (immediate; flushes batched geometry first).
     *
     * @param holder    living entity for glint seed
     * @param stack     item stack to draw
     * @param positionX left origin in logical pixels
     * @param positionY top origin in logical pixels
     */
    public void drawGuiItem(LivingEntity holder, ItemStack stack, float positionX, float positionY) {
        backend.drawGuiItem(holder, stack, positionX, positionY);
    }

    private void recycleTextOperation(GuiRenderOperation<?> operation) {
        if (operation instanceof TextOperation textOperation) {
            if (textOperationPool.size() < TEXT_OPERATION_POOL_CAP) {
                textOperationPool.addFirst(textOperation);
            }
        }
        else if (operation instanceof MiniMessageTextOperation miniMessageTextOperation) {
            if (miniMessageOperationPool.size() < TEXT_OPERATION_POOL_CAP) {
                miniMessageOperationPool.addFirst(miniMessageTextOperation);
            }
        }
    }

    private TextOperation borrowTextOperation() {
        TextOperation pooled = textOperationPool.pollFirst();
        return pooled != null ? pooled : new TextOperation();
    }

    private MiniMessageTextOperation borrowMiniMessageOperation() {
        MiniMessageTextOperation pooled = miniMessageOperationPool.pollFirst();
        return pooled != null ? pooled : new MiniMessageTextOperation();
    }

    private void queueText(GuiRenderOperation<?> operation) {
        textQueue.add(operation);
    }
}
