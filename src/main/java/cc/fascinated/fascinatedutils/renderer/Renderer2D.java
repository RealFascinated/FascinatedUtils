package cc.fascinated.fascinatedutils.renderer;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.common.LRUCache;
import cc.fascinated.fascinatedutils.oldgui.GuiTheme;
import cc.fascinated.fascinatedutils.oldgui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.oldgui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.renderer.text.TextRenderer;
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;

import java.util.function.Supplier;

/**
 * Low-level GUI draw bridge: batched mesh quads (rounded rects, gradients, {@link #drawBorder}) via {@link MeshBuilder}
 * / {@link MeshRenderer}, then {@link GuiGraphicsExtractor} immediate draws for text and items. Text uses the active
 * {@link GuiTheme#textRenderer}.
 */
public class Renderer2D {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LRUCache<String, net.minecraft.network.chat.Component> MINI_MESSAGE_CACHE = new LRUCache<>(1024);
    private final GuiGraphicsExtractor graphics;
    private final GuiTheme guiTheme;
    private final TextRenderer guiTextRenderer;
    private final Supplier<ScreenRectangle> scissorSupplier;
    private float multiplyAlpha = 1f;
    private float multiplyAlphaText = 1f;

    public Renderer2D(GuiGraphicsExtractor graphics, GuiTheme guiTheme, Supplier<ScreenRectangle> scissorSupplier) {
        this.graphics = graphics;
        this.guiTheme = guiTheme;
        this.guiTextRenderer = guiTheme.textRenderer(graphics);
        this.scissorSupplier = scissorSupplier;
    }

    /**
     * Stroke thickness applied inside {@link #fillRoundedRectFrame}; use the same value to inset content that must sit
     * flush with that pass's inner fill.
     *
     * @param borderThicknessX requested horizontal stroke in logical pixels
     * @param borderThicknessY requested vertical stroke in logical pixels
     * @return the larger of one pixel and the rounded minimum of the two requested strokes
     */
    public static float roundedRectFrameBorderThickness(float borderThicknessX, float borderThicknessY) {
        return Math.max(1f, Math.round(Math.min(borderThicknessX, borderThicknessY)));
    }

    private static int scaleColorAlpha(int color, float factor) {
        if (factor >= 0.999f) {
            return color;
        }
        int alpha255 = (color >>> 24) & 0xFF;
        int scaledAlpha255 = Mth.clamp(Mth.floor(alpha255 * factor), 0, 255);
        return (scaledAlpha255 << 24) | (color & 0xFFFFFF);
    }

    /**
     * Average length of the pose image of the unit X and Y axes (translation cancels). Matches how {@link
     * GuiGraphics#fill} thickness scales with the matrix stack; the rounded-rect ring shader measures stroke in
     * framebuffer pixels, so callers pass logical stroke and multiply by this before LUT upload.
     */
    private static float linearUniformScaleFromModelPose(Matrix3x2f pose) {
        Vector2f origin = new Vector2f(0f, 0f);
        Vector2f unitX = new Vector2f(1f, 0f);
        Vector2f unitY = new Vector2f(0f, 1f);
        pose.transformPosition(origin);
        pose.transformPosition(unitX);
        pose.transformPosition(unitY);
        unitX.sub(origin);
        unitY.sub(origin);
        return 0.5f * (unitX.length() + unitY.length());
    }

    /**
     * @return theme bound to this renderer for the current frame
     */
    public GuiTheme guiTheme() {
        return guiTheme;
    }

    public void setMultiplyAlpha(float factor) {
        float clamped = Mth.clamp(factor, 0f, 1f);
        this.multiplyAlpha = clamped;
        this.multiplyAlphaText = clamped;
    }

    /**
     * Set separate alpha multipliers for non-text draws (fills, textures, borders) and for text draws.
     *
     * @param geometryFactor multiplier applied to shapes and textures
     * @param textFactor     multiplier applied to string and {@link net.minecraft.network.chat.Component} draws
     */
    public void setMultiplyAlpha(float geometryFactor, float textFactor) {
        this.multiplyAlpha = Mth.clamp(geometryFactor, 0f, 1f);
        this.multiplyAlphaText = Mth.clamp(textFactor, 0f, 1f);
    }

    public void resetMultiplyAlpha() {
        this.multiplyAlpha = 1f;
        this.multiplyAlphaText = 1f;
    }

    /**
     * Apply the current text alpha multiplier for queued deferred draws (see {@link
     * GuiRenderer}).
     *
     * @param color packed ARGB input color
     * @return packed ARGB color with text alpha multiplier applied
     */
    public int withQueuedTextColor(int color) {
        return withTextAlpha(color);
    }

    public void drawRect(float positionX, float positionY, float width, float height, int color) {
        if (width < 1e-3f || height < 1e-3f) {
            return;
        }
        int argb = withAlpha(color);
        MeshRenderer.INSTANCE.enqueueAxisTexQuad(graphics, currentScissor(), positionX, positionY, width, height, argb, argb, argb, argb);
    }

    public void drawLine(float x1, float y1, float x2, float y2, float thickness, int color) {
        float deltaX = x2 - x1;
        float deltaY = y2 - y1;
        float length = (float) Math.hypot(deltaX, deltaY);
        if (length < 1e-4f) {
            drawRect(x1, y1, thickness, thickness, color);
            return;
        }
        float midX = (x1 + x2) * 0.5f;
        float midY = (y1 + y2) * 0.5f;
        float angle = (float) Math.atan2(deltaY, deltaX);
        Matrix3x2fStack matrices = graphics.pose();
        matrices.pushMatrix();
        matrices.translate(midX, midY);
        matrices.rotate(angle);
        drawRect(-length * 0.5f, -thickness * 0.5f, length, thickness, color);
        matrices.popMatrix();
    }

    public void fillGradientVertical(float positionX, float positionY, float width, float height, int colorTop, int colorBottom) {
        if (width < 1e-3f || height < 1e-3f) {
            return;
        }
        int top = withAlpha(colorTop);
        int bottom = withAlpha(colorBottom);
        MeshRenderer.INSTANCE.enqueueAxisTexQuad(graphics, currentScissor(), positionX, positionY, width, height, top, bottom, bottom, top);
    }

    public void fillRoundedGradientVertical(float positionX, float positionY, float width, float height, float cornerRadius, int colorTop, int colorBottom, int cornerRoundMask) {
        MeshRenderer.INSTANCE.enqueueRoundedGradient(graphics, currentScissor(), positionX, positionY, width, height, cornerRadius, colorTop, colorBottom, cornerRoundMask);
    }

    public void drawBorder(float positionX, float positionY, float width, float height, float thickness, int color) {
        if (width < 1e-3f || height < 1e-3f) {
            return;
        }
        float strokeLogical = Math.max(1f, Math.round(thickness));
        enqueueLutBorderRing(positionX, positionY, width, height, 0f, strokeLogical, withAlpha(color), RectCornerRoundMask.ALL);
    }

    public void fillRoundedRect(float positionX, float positionY, float width, float height, float cornerRadius, int fillArgb, int cornerRoundMask) {
        MeshRenderer.INSTANCE.enqueueRoundedGradient(graphics, currentScissor(), positionX, positionY, width, height, cornerRadius, fillArgb, fillArgb, cornerRoundMask, 0f);
    }

    /**
     * LUT rounded-rect path that tints only an outer band of {@code ringStrokePx} (same SDF as fills, no inner
     * overdraw). Intended for HUD chrome drawn after a separate fill pass.
     */
    public void fillRoundedRectBorderRing(float positionX, float positionY, float width, float height, float cornerRadius, float ringStrokePx, int borderArgb, int cornerRoundMask) {
        enqueueLutBorderRing(positionX, positionY, width, height, cornerRadius, ringStrokePx, withAlpha(borderArgb), cornerRoundMask);
    }

    public void fillRoundedRectFrame(float positionX, float positionY, float width, float height, float cornerRadius, int borderArgb, int fillArgb, float borderThicknessX, float borderThicknessY, int cornerRoundMask) {
        float thickness = roundedRectFrameBorderThickness(borderThicknessX, borderThicknessY);
        ScreenRectangle scissor = currentScissor();
        MeshRenderer.INSTANCE.enqueueRoundedGradient(graphics, scissor, positionX, positionY, width, height, cornerRadius, borderArgb, borderArgb, cornerRoundMask, 0f);
        float innerX = positionX + thickness;
        float innerY = positionY + thickness;
        float innerW = Math.max(0f, width - 2f * thickness);
        float innerH = Math.max(0f, height - 2f * thickness);
        float innerRadius = Math.max(0f, cornerRadius - thickness);
        if (innerW > 0.5f && innerH > 0.5f) {
            MeshRenderer.INSTANCE.enqueueRoundedGradient(graphics, scissor, innerX, innerY, innerW, innerH, innerRadius, fillArgb, fillArgb, cornerRoundMask, 0f);
        }
    }

    /**
     * Draw styled {@link net.minecraft.network.chat.Component} after batched geometry has been flushed (Meteor text pass); no extra batch flush.
     *
     * @param text      rich text to draw
     * @param positionX left origin in logical pixels
     * @param positionY layout line top in logical pixels (top of the nominal line box; converted to vanilla {@code drawString}-compatible baseline coordinates before drawing)
     * @param color     packed ARGB color (already multiplied when queued from {@link
     *                  GuiRenderer})
     * @param shadow    whether to draw vanilla text shadow
     */
    public void drawTextImmediate(net.minecraft.network.chat.Component text, float positionX, float positionY, int color, boolean shadow) {
        float sx = shellScaleX();
        float sy = shellScaleY();
        Matrix3x2fStack matrices = graphics.pose();
        int originX = Mth.floor(positionX);
        int originY = Math.round(positionY);
        if (Math.abs(sx - 1f) > 1e-4f || Math.abs(sy - 1f) > 1e-4f) {
            matrices.pushMatrix();
            matrices.translate(originX, originY);
            matrices.scale(sx, sy);
            guiTextRenderer.drawText(graphics, text, 0, 0, color, shadow);
            matrices.popMatrix();
        }
        else {
            guiTextRenderer.drawText(graphics, text, originX, originY, color, shadow);
        }
    }

    public net.minecraft.network.chat.Component parseMiniMessage(String miniMessageText) {
        net.minecraft.network.chat.Component component = MINI_MESSAGE_CACHE.get(miniMessageText);
        if (component != null) {
            return component;
        }

        ProfilerFiller profiler = Profiler.get();
        try {
            profiler.push("parseMiniMessage");
            Component adventure = MINI_MESSAGE.deserialize(miniMessageText == null ? "" : miniMessageText);
            net.minecraft.network.chat.Component nativeComponent = MinecraftClientAudiences.of().asNative(adventure);
            MINI_MESSAGE_CACHE.put(miniMessageText, nativeComponent);
            profiler.pop();
            return nativeComponent;
        } catch (Exception exception) {
            Client.LOG.debug("MiniMessage parse failed for UI text \"{}\", falling back to literal", miniMessageText, exception);
            return net.minecraft.network.chat.Component.literal(miniMessageText == null ? "" : miniMessageText);
        }
    }

    /**
     * Measure {@link net.minecraft.network.chat.Component} width in logical pixels (matches {@link #drawTextImmediate}).
     */
    public int measureTextWidth(net.minecraft.network.chat.Component text) {
        int vanilla = guiTextRenderer.getWidth(text);
        return Math.max(1, Mth.ceil(vanilla * shellScaleX()));
    }

    /**
     * Draw a GUI item like a hotbar slot: stack icon plus vanilla overlays (stack count, durability bar, cooldown).
     *
     * @param stack     item stack to draw (empty stacks are ignored)
     * @param positionX left origin in logical pixels
     * @param positionY top origin in logical pixels
     */
    public void drawGuiItem(ItemStack stack, float positionX, float positionY) {
        if (stack.isEmpty()) {
            return;
        }
        flushMeshSegmentBeforeImmediateDraw();
        int originX = Mth.floor(positionX);
        int originY = Mth.floor(positionY);
        Font vanillaFont = Minecraft.getInstance().font;
        graphics.fakeItem(stack, originX, originY);
        graphics.itemDecorations(vanillaFont, stack, originX, originY);
    }

    /**
     * Draw a GUI item like a hotbar slot for a holder: icon, glint seed, then vanilla overlays (count, bar, cooldown).
     *
     * @param holder    living entity whose age contributes to the render seed
     * @param stack     item stack to draw (empty stacks are ignored)
     * @param positionX left origin in logical pixels
     * @param positionY top origin in logical pixels
     */
    public void drawGuiItem(LivingEntity holder, ItemStack stack, float positionX, float positionY) {
        if (stack.isEmpty()) {
            return;
        }
        flushMeshSegmentBeforeImmediateDraw();
        int originX = Mth.floor(positionX);
        int originY = Mth.floor(positionY);
        Font vanillaFont = Minecraft.getInstance().font;
        int seed = Math.floorMod(holder.tickCount * 31 + stack.hashCode(), Integer.MAX_VALUE);
        graphics.item(holder, stack, originX, originY, seed);
        graphics.itemDecorations(vanillaFont, stack, originX, originY);
    }

    /**
     * Draw a literal string after batched geometry has been flushed; {@code color} is final packed ARGB.
     *
     * @param text      string content
     * @param positionX left origin in logical pixels
     * @param positionY top origin in logical pixels
     * @param color     packed ARGB color
     * @param shadow    whether vanilla text shadow is drawn
     * @param bold      whether bold styling is applied
     */
    public void drawTextImmediate(String text, float positionX, float positionY, int color, boolean shadow, boolean bold) {
        float sx = shellScaleX();
        float sy = shellScaleY();
        Matrix3x2fStack matrices = graphics.pose();
        FormattedCharSequence literalText = styledLiteralSequence(text, bold);
        if (Math.abs(sx - 1f) > 1e-4f || Math.abs(sy - 1f) > 1e-4f) {
            int originX = Mth.floor(positionX);
            int originY = Math.round(positionY);
            matrices.pushMatrix();
            matrices.translate(originX, originY);
            matrices.scale(sx, sy);
            guiTextRenderer.drawText(graphics, literalText, 0, 0, color, shadow);
            matrices.popMatrix();
        }
        else {
            int originX = Mth.floor(positionX);
            int originY = Math.round(positionY);
            guiTextRenderer.drawText(graphics, literalText, originX, originY, color, shadow);
        }
    }

    public void drawTexture(Identifier texture, float positionX, float positionY, float width, float height, int tintArgb) {
        if (width < 1e-3f || height < 1e-3f) {
            return;
        }
        Object raw = Minecraft.getInstance().getTextureManager().getTexture(texture);
        if (raw instanceof DynamicTexture dynamicTexture) {
            MeshRenderer.INSTANCE.enqueueTexturedQuad(graphics, currentScissor(), dynamicTexture, positionX, positionY, width, height, withAlpha(tintArgb));
            return;
        }
        flushMeshSegmentBeforeImmediateDraw();
        int drawWidth = Math.max(1, Math.round(width));
        int drawHeight = Math.max(1, Math.round(height));
        Matrix3x2fStack matrices = graphics.pose();
        matrices.pushMatrix();
        matrices.translate(positionX, positionY);
        ModUiTextures.drawTinted(graphics, texture, 0, 0, drawWidth, drawHeight, withAlpha(tintArgb));
        matrices.popMatrix();
    }

    public void drawRoundedTexture(Identifier texture, float positionX, float positionY, float width, float height, float cornerRadius, int tintArgb) {
        if (width < 1e-3f || height < 1e-3f) {
            return;
        }
        Object raw = Minecraft.getInstance().getTextureManager().getTexture(texture);
        if (raw instanceof DynamicTexture dynamicTexture) {
            MeshRenderer.INSTANCE.enqueueRoundedTexturedQuad(graphics, currentScissor(), dynamicTexture, positionX, positionY, width, height, cornerRadius, withAlpha(tintArgb));
            return;
        }
        drawTexture(texture, positionX, positionY, width, height, tintArgb);
    }

    public void drawSprite(Identifier spriteId, float positionX, float positionY, float width, float height, int tintArgb) {
        if (width < 1e-3f || height < 1e-3f) {
            return;
        }
        flushMeshSegmentBeforeImmediateDraw();
        int drawWidth = Math.max(1, Math.round(width));
        int drawHeight = Math.max(1, Math.round(height));
        Matrix3x2fStack matrices = graphics.pose();
        matrices.pushMatrix();
        matrices.translate(positionX, positionY);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteId, 0, 0, drawWidth, drawHeight, withAlpha(tintArgb));
        matrices.popMatrix();
    }

    public void drawSprite(Identifier spriteId, int textureWidth, int textureHeight, int sourceX, int sourceY, float positionX, float positionY, float width, float height) {
        if (width < 1e-3f || height < 1e-3f) {
            return;
        }
        flushMeshSegmentBeforeImmediateDraw();
        int drawWidth = Math.max(1, Math.round(width));
        int drawHeight = Math.max(1, Math.round(height));
        Matrix3x2fStack matrices = graphics.pose();
        matrices.pushMatrix();
        matrices.translate(positionX, positionY);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteId, textureWidth, textureHeight, sourceX, sourceY, 0, 0, drawWidth, drawHeight);
        matrices.popMatrix();
    }

    public void pushTranslate(float offsetX, float offsetY) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(offsetX, offsetY);
    }

    public void popTranslate() {
        graphics.pose().popMatrix();
    }

    public void pushScale(float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
    }

    public void popScale() {
        graphics.pose().popMatrix();
    }

    public int measureTextWidth(String text, boolean bold) {
        double vanilla = guiTextRenderer.getWidth(styledLiteralSequence(text, bold));
        return Math.max(1, Mth.ceil(vanilla * shellScaleX()));
    }

    public int getFontHeight() {
        return Math.max(1, Mth.ceil(guiTextRenderer.getHeight() * shellScaleY()));
    }

    private float shellScaleX() {
        return 1f;
    }

    private float shellScaleY() {
        return 1f;
    }

    private void flushMeshSegmentBeforeImmediateDraw() {
        MeshBuilder.INSTANCE.endSegment(graphics);
        MeshBuilder.INSTANCE.beginSegment(graphics);
    }

    private ScreenRectangle currentScissor() {
        return scissorSupplier == null ? null : scissorSupplier.get();
    }

    private int withAlpha(int color) {
        return scaleColorAlpha(color, multiplyAlpha);
    }

    private int withTextAlpha(int color) {
        return scaleColorAlpha(color, multiplyAlphaText);
    }

    /**
     * Queues {@code fui_rounded_rect} outer-ring mode (LUT): same path as {@link #fillRoundedRectBorderRing}; stroke is
     * in logical pixels, constant in framebuffer pixels regardless of GUI scale (scales only with widget user scale).
     */
    private void enqueueLutBorderRing(float positionX, float positionY, float width, float height, float cornerRadius, float ringStrokeLogicalPx, int borderArgb, int cornerRoundMask) {
        if (width < 1e-3f || height < 1e-3f || ringStrokeLogicalPx <= 0f) {
            return;
        }
        Matrix3x2f poseSnapshot = new Matrix3x2f(graphics.pose());
        float poseScale = Math.max(0.001f, linearUniformScaleFromModelPose(poseSnapshot));
        // The pose base scale from GuiRenderer.begin() is 2/guiScale; multiply by guiScale to cancel it,
        // leaving widgetUserScale * 2 (since 1 logical px = 2 framebuffer pixels on the fixed scale-2 canvas).
        Minecraft mc = Minecraft.getInstance();
        float guiScale = mc.getWindow().getWidth() / Math.max(1f, (float) mc.getWindow().getGuiScaledWidth());
        float strokeForLut = ringStrokeLogicalPx * poseScale * guiScale * 0.5f;
        MeshRenderer.INSTANCE.enqueueRoundedGradient(graphics, currentScissor(), positionX, positionY, width, height, cornerRadius, borderArgb, borderArgb, cornerRoundMask, strokeForLut);
    }

    private FormattedCharSequence styledLiteralSequence(String raw, boolean bold) {
        Style style = bold ? Style.EMPTY.withBold(true) : Style.EMPTY;
        return FormattedCharSequence.forward(raw == null ? "" : raw, style);
    }
}
