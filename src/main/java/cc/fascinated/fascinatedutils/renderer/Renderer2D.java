package cc.fascinated.fascinatedutils.renderer;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.renderer.text.GlyphAtlas;
import cc.fascinated.fascinatedutils.renderer.text.TextCaches;
import cc.fascinated.fascinatedutils.renderer.text.TextVariant;
import cc.fascinated.fascinatedutils.renderer.text.VanillaTextRenderer;
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * GUI draw primitives. Every call is submitted immediately with the pose and scissor of the draw context at call time,
 * so paint order is call order. Geometry goes through {@link MeshRenderer}; text, sprites and items go through the
 * draw context. Frame state (pose stack, clips, alpha stack) lives in
 * {@code AlumiteGuiRenderer}, which tells this class the alpha factor to apply through {@link #setAlpha}.
 *
 * <p>Sizes and positions are in local units, that is, whatever the current pose maps to the screen. Rectangle edges are
 * rounded to whole local units.
 */
public class Renderer2D {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final TextCaches TEXT_CACHES = new TextCaches();
    private static final VanillaTextRenderer TEXT_RENDERER = new VanillaTextRenderer();
    private static final Map<Identifier, AbstractTexture> TEXTURE_LOOKUP = new HashMap<>();
    private static final int OPAQUE_WHITE = 0xFFFFFFFF;
    private static final int ITEM_BAR_BACKGROUND = 0xFF000000;
    private static final int ITEM_COOLDOWN_OVERLAY = 0x7FFFFFFF;
    /**
     * Alpha multiplier captured by {@code GuiItemRenderState} instances created right now. Only non-default between
     * the set and reset around an item submission in this class.
     */
    private static float guiItemAlpha = 1f;
    private GuiGraphicsExtractor graphics;
    private boolean guiFont;
    private final MeshRenderer mesh;
    private float alpha = 1f;

    /**
     * Creates a renderer.
     *
     * @param graphics the draw context to submit to
     * @param guiFont  whether literal text uses the mod's GUI font
     */
    public Renderer2D(GuiGraphicsExtractor graphics, boolean guiFont) {
        this.graphics = graphics;
        this.guiFont = guiFont;
        this.mesh = new MeshRenderer(graphics);
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

    /**
     * Alpha multiplier to capture on GUI items submitted right now; read by the item render state mixin.
     *
     * @return the alpha in the range 0 to 1
     */
    public static float guiItemAlpha() {
        return guiItemAlpha;
    }

    /**
     * Drops every cached MiniMessage and literal width and sequence; call when fonts or languages may have changed.
     */
    public static void clearMiniMessageWidthCache() {
        TEXT_CACHES.clear();
    }

    private static int scaleColorAlpha(int color, float factor) {
        if (factor >= 0.999f) {
            return color;
        }
        int scaledAlpha = Mth.clamp(Math.round(((color >>> 24) & 0xFF) * factor), 0, 255);
        return (scaledAlpha << 24) | (color & 0xFFFFFF);
    }

    /**
     * Vanilla treats text alpha below four as fully opaque, so text that faded that far must be skipped instead.
     */
    private static boolean isTextInvisible(int argb) {
        return (argb & 0xFC000000) == 0;
    }

    private static boolean isInvisible(int argb) {
        return (argb >>> 24) == 0;
    }

    /**
     * Points this renderer at another draw context so one instance can be reused across frames.
     *
     * @param graphics the new draw context
     * @param guiFont  whether literal text uses the mod's GUI font
     */
    public void rebind(GuiGraphicsExtractor graphics, boolean guiFont) {
        this.graphics = graphics;
        this.guiFont = guiFont;
        this.mesh.rebind(graphics);
    }

    /**
     * Starts a frame: forgets texture lookups from the previous frame, which may have been replaced by a reload.
     */
    public void beginFrame() {
        TEXTURE_LOOKUP.clear();
    }

    /**
     * Sets the alpha multiplier applied to everything drawn from now on.
     *
     * @param alphaFactor multiplier in the range 0 to 1
     */
    public void setAlpha(float alphaFactor) {
        this.alpha = Mth.clamp(alphaFactor, 0f, 1f);
    }

    /**
     * Fills a rectangle.
     *
     * @param positionX local left
     * @param positionY local top
     * @param width     local width
     * @param height    local height
     * @param color     packed ARGB
     */
    public void drawRect(float positionX, float positionY, float width, float height, int color) {
        int argb = withAlpha(color);
        if (width < 1e-3f || height < 1e-3f || isInvisible(argb)) {
            return;
        }
        mesh.fillRect(positionX, positionY, width, height, argb, argb, argb, argb);
    }

    /**
     * Draws a dotted rectangle outline from the local origin.
     *
     * @param width   outline width
     * @param height  outline height
     * @param dotSize dot side length
     * @param step    distance between dot starts
     * @param color   packed ARGB
     */
    public void drawDottedOutline(float width, float height, float dotSize, float step, int color) {
        int argb = withAlpha(color);
        if (isInvisible(argb)) {
            return;
        }
        mesh.dottedOutline(width, height, dotSize, step, argb);
    }

    /**
     * Fills a rectangle with a vertical gradient.
     *
     * @param positionX   local left
     * @param positionY   local top
     * @param width       local width
     * @param height      local height
     * @param colorTop    packed ARGB at the top
     * @param colorBottom packed ARGB at the bottom
     */
    public void fillGradientVertical(float positionX, float positionY, float width, float height, int colorTop, int colorBottom) {
        int top = withAlpha(colorTop);
        int bottom = withAlpha(colorBottom);
        if (width < 1e-3f || height < 1e-3f || (isInvisible(top) && isInvisible(bottom))) {
            return;
        }
        mesh.fillRect(positionX, positionY, width, height, top, bottom, bottom, top);
    }

    /**
     * Draws a square-cornered border inside the rectangle.
     *
     * @param positionX local left
     * @param positionY local top
     * @param width     local width
     * @param height    local height
     * @param thickness requested thickness; rounded, at least one
     * @param color     packed ARGB
     */
    public void drawBorder(float positionX, float positionY, float width, float height, float thickness, int color) {
        int argb = withAlpha(color);
        if (width < 1e-3f || height < 1e-3f || isInvisible(argb)) {
            return;
        }
        mesh.border(positionX, positionY, width, height, thickness, argb);
    }

    /**
     * Fills a rounded rectangle.
     *
     * @param positionX       local left
     * @param positionY       local top
     * @param width           local width
     * @param height          local height
     * @param cornerRadius    corner radius
     * @param fillArgb        packed ARGB
     * @param cornerRoundMask which corners are rounded
     */
    public void fillRoundedRect(float positionX, float positionY, float width, float height, float cornerRadius, int fillArgb, int cornerRoundMask) {
        int argb = withAlpha(fillArgb);
        if (width < 1e-3f || height < 1e-3f || isInvisible(argb)) {
            return;
        }
        mesh.roundedRect(positionX, positionY, width, height, cornerRadius, argb, argb, cornerRoundMask, 0f);
    }

    /**
     * Draws only an outer band of the rounded rectangle, for stacking on a separate fill pass. The stroke is measured
     * in local units, so it scales with the pose like the rectangle itself.
     *
     * @param positionX       local left
     * @param positionY       local top
     * @param width           local width
     * @param height          local height
     * @param cornerRadius    corner radius
     * @param ringStroke      band thickness
     * @param borderArgb      packed ARGB
     * @param cornerRoundMask which corners are rounded
     */
    public void fillRoundedRectBorderRing(float positionX, float positionY, float width, float height, float cornerRadius, float ringStroke, int borderArgb, int cornerRoundMask) {
        int argb = withAlpha(borderArgb);
        if (width < 1e-3f || height < 1e-3f || ringStroke <= 0f || isInvisible(argb)) {
            return;
        }
        mesh.roundedRect(positionX, positionY, width, height, cornerRadius, argb, argb, cornerRoundMask, ringStroke);
    }

    /**
     * Draws a rounded rectangle as a border-colored shape with a smaller fill-colored shape on top.
     *
     * @param positionX        local left
     * @param positionY        local top
     * @param width            local width
     * @param height           local height
     * @param cornerRadius     outer corner radius
     * @param borderArgb       packed ARGB of the border
     * @param fillArgb         packed ARGB of the inside
     * @param borderThicknessX requested horizontal border thickness
     * @param borderThicknessY requested vertical border thickness
     * @param cornerRoundMask  which corners are rounded
     */
    public void fillRoundedRectFrame(float positionX, float positionY, float width, float height, float cornerRadius, int borderArgb, int fillArgb, float borderThicknessX, float borderThicknessY, int cornerRoundMask) {
        if (width < 1e-3f || height < 1e-3f) {
            return;
        }
        float thickness = roundedRectFrameBorderThickness(borderThicknessX, borderThicknessY);
        int border = withAlpha(borderArgb);
        int fill = withAlpha(fillArgb);
        if (!isInvisible(border)) {
            mesh.roundedRect(positionX, positionY, width, height, cornerRadius, border, border, cornerRoundMask, 0f);
        }
        float innerWidth = width - 2f * thickness;
        float innerHeight = height - 2f * thickness;
        if (innerWidth > 0.5f && innerHeight > 0.5f && !isInvisible(fill)) {
            mesh.roundedRect(positionX + thickness, positionY + thickness, innerWidth, innerHeight, Math.max(0f, cornerRadius - thickness), fill, fill, cornerRoundMask, 0f);
        }
    }

    /**
     * Draws rich text with the alpha applied.
     *
     * @param text      rich text
     * @param positionX left origin
     * @param positionY layout line top
     * @param color     packed ARGB before alpha
     * @param shadow    whether to draw vanilla text shadow
     */
    public void drawText(Component text, float positionX, float positionY, int color, boolean shadow) {
        int argb = withAlpha(color);
        if (isTextInvisible(argb)) {
            return;
        }
        TEXT_RENDERER.draw(graphics, text, positionX, positionY, argb, shadow);
    }

    /**
     * Draws a pre-ordered glyph sequence with the alpha applied.
     *
     * @param text      visual-order glyph sequence
     * @param positionX left origin
     * @param positionY layout line top
     * @param color     packed ARGB before alpha
     * @param shadow    whether vanilla text shadow is drawn
     */
    public void drawText(FormattedCharSequence text, float positionX, float positionY, int color, boolean shadow) {
        int argb = withAlpha(color);
        if (isTextInvisible(argb)) {
            return;
        }
        TEXT_RENDERER.draw(graphics, text, positionX, positionY, argb, shadow);
    }

    /**
     * Draws a literal string with the alpha applied. With the GUI font, strings the glyph atlas covers are drawn
     * from it (without shadow); everything else uses the vanilla font.
     *
     * @param text      string content
     * @param positionX left origin
     * @param positionY layout line top
     * @param color     packed ARGB before alpha
     * @param shadow    whether vanilla shadow is drawn
     * @param bold      whether bold styling is applied
     */
    public void drawText(String text, float positionX, float positionY, int color, boolean shadow, boolean bold) {
        int argb = withAlpha(color);
        if (text == null || text.isEmpty() || isTextInvisible(argb)) {
            return;
        }
        if (guiFont) {
            GlyphAtlas atlas = GlyphAtlas.get(bold);
            if (atlas.canRender(text)) {
                atlas.draw(mesh, text, positionX, positionY, argb);
                return;
            }
        }
        TEXT_RENDERER.draw(graphics, TEXT_CACHES.literal(TextVariant.of(guiFont, bold), text), positionX, positionY, argb, shadow);
    }

    /**
     * Parses MiniMessage into a component. Results, including the literal fallback for unparsable text, are cached.
     *
     * @param miniMessageText MiniMessage source; null is treated as empty
     * @return the parsed component
     */
    public Component parseMiniMessage(String miniMessageText) {
        String source = miniMessageText == null ? "" : miniMessageText;
        Component component = TEXT_CACHES.miniMessageComponent(source);
        if (component != null) {
            return component;
        }
        ProfilerFiller profiler = Profiler.get();
        profiler.push("parseMiniMessage");
        try {
            component = MinecraftClientAudiences.of().asNative(MINI_MESSAGE.deserialize(source));
        }
        catch (Exception exception) {
            Client.LOG.debug("MiniMessage parse failed for UI text \"{}\", falling back to literal", source, exception);
            component = Component.literal(source);
        }
        finally {
            profiler.pop();
        }
        TEXT_CACHES.putMiniMessageComponent(source, component);
        return component;
    }

    /**
     * Measures the width of MiniMessage text, reusing the last measurement for the exact same text.
     *
     * @param miniMessageText MiniMessage source text
     * @return width in logical pixels, at least one
     */
    public int measureMiniMessageTextWidth(String miniMessageText) {
        String source = miniMessageText == null ? "" : miniMessageText;
        Integer cachedWidth = TEXT_CACHES.miniMessageWidth(source);
        if (cachedWidth != null) {
            return cachedWidth;
        }
        int width = measureTextWidth(parseMiniMessage(source));
        TEXT_CACHES.putMiniMessageWidth(source, width);
        return width;
    }

    /**
     * Returns the ordered glyph sequence for MiniMessage text, reusing the last conversion for the exact same text.
     * Vanilla rebuilds this sequence from a {@link Component} on every draw, so caching it avoids that per-frame work.
     *
     * @param miniMessageText MiniMessage source text
     * @return visual-order sequence ready for {@link #drawText(FormattedCharSequence, float, float, int, boolean)}
     */
    public FormattedCharSequence miniMessageVisualOrder(String miniMessageText) {
        String source = miniMessageText == null ? "" : miniMessageText;
        FormattedCharSequence sequence = TEXT_CACHES.miniMessageSequence(source);
        if (sequence == null) {
            sequence = parseMiniMessage(source).getVisualOrderText();
            TEXT_CACHES.putMiniMessageSequence(source, sequence);
        }
        return sequence;
    }

    /**
     * Measures a component, matching {@link #drawText(Component, float, float, int, boolean)}.
     *
     * @param text rich text to measure
     * @return width in logical pixels, at least one
     */
    public int measureTextWidth(Component text) {
        return TEXT_RENDERER.width(text);
    }

    /**
     * Measures a literal string in the style {@link #drawText(String, float, float, int, boolean, boolean)} draws it.
     * Results are cached because layout re-measures the same labels every frame.
     *
     * @param text the text to measure
     * @param bold whether the text is bold
     * @return the width in logical pixels, at least one
     */
    public int measureTextWidth(String text, boolean bold) {
        String plainText = text == null ? "" : text;
        TextVariant variant = TextVariant.of(guiFont, bold);
        Integer cachedWidth = TEXT_CACHES.width(variant, plainText);
        if (cachedWidth != null) {
            return cachedWidth;
        }
        int width;
        GlyphAtlas atlas = guiFont ? GlyphAtlas.get(bold) : null;
        if (atlas != null && atlas.canRender(plainText)) {
            width = Math.max(1, Mth.ceil(atlas.measure(plainText)));
        }
        else {
            width = TEXT_RENDERER.width(TEXT_CACHES.literal(variant, plainText));
        }
        TEXT_CACHES.putWidth(variant, plainText, width);
        return width;
    }

    /**
     * @return the nominal line height of body text, at least one
     */
    public int getFontHeight() {
        return Math.max(1, TEXT_RENDERER.lineHeight());
    }

    /**
     * Draws a GUI item like a hotbar slot: the stack icon plus count, durability bar and cooldown, all faded by the
     * alpha.
     *
     * @param stack     item stack to draw (empty stacks are ignored)
     * @param positionX left origin
     * @param positionY top origin
     */
    public void drawGuiItem(ItemStack stack, float positionX, float positionY) {
        if (stack.isEmpty() || alpha <= 0f) {
            return;
        }
        int originX = Math.round(positionX);
        int originY = Math.round(positionY);
        guiItemAlpha = alpha;
        try {
            graphics.fakeItem(stack, originX, originY);
        }
        finally {
            guiItemAlpha = 1f;
        }
        drawItemDecorations(stack, originX, originY);
    }

    /**
     * Draws a GUI item for a holder: icon, glint seed, then count, durability bar and cooldown.
     *
     * @param holder    living entity whose age contributes to the render seed
     * @param stack     item stack to draw (empty stacks are ignored)
     * @param positionX left origin
     * @param positionY top origin
     */
    public void drawGuiItem(LivingEntity holder, ItemStack stack, float positionX, float positionY) {
        if (stack.isEmpty() || alpha <= 0f) {
            return;
        }
        int originX = Math.round(positionX);
        int originY = Math.round(positionY);
        int seed = Math.floorMod(holder.tickCount * 31 + stack.hashCode(), Integer.MAX_VALUE);
        guiItemAlpha = alpha;
        try {
            graphics.item(holder, stack, originX, originY, seed);
        }
        finally {
            guiItemAlpha = 1f;
        }
        drawItemDecorations(stack, originX, originY);
    }

    /**
     * Draws a texture over a rectangle, sampled linearly.
     *
     * @param texture   id of a texture registered with the texture manager
     * @param positionX local left
     * @param positionY local top
     * @param width     local width
     * @param height    local height
     * @param tintArgb  packed ARGB tint
     */
    public void drawTexture(Identifier texture, float positionX, float positionY, float width, float height, int tintArgb) {
        int tint = withAlpha(tintArgb);
        if (width < 1e-3f || height < 1e-3f || isInvisible(tint)) {
            return;
        }
        mesh.texturedRect(MeshRenderer.linearTextureSetup(lookupTexture(texture)), positionX, positionY, width, height, tint);
    }

    /**
     * Draws a sub-rectangle of a texture over a rounded rectangle, so the rounded mask applies to the destination
     * rectangle while a cropped region is sampled; pass {@code 0, 0, 1, 1} for the whole texture. The texture is sampled
     * linearly.
     *
     * @param texture      texture or sprite id
     * @param positionX    local left
     * @param positionY    local top
     * @param width        local width
     * @param height       local height
     * @param cornerRadius corner radius
     * @param tintArgb     packed ARGB tint
     * @param textureU0    texture u at the left edge, 0 to 1 across the full texture
     * @param textureV0    texture v at the top edge
     * @param textureU1    texture u at the right edge
     * @param textureV1    texture v at the bottom edge
     */
    public void drawRoundedTexture(Identifier texture, float positionX, float positionY, float width, float height, float cornerRadius, int tintArgb, float textureU0, float textureV0, float textureU1, float textureV1) {
        int tint = withAlpha(tintArgb);
        if (width < 1e-3f || height < 1e-3f || isInvisible(tint)) {
            return;
        }
        mesh.roundedTexturedRect(MeshRenderer.linearTextureSetup(lookupTexture(texture)), positionX, positionY, width, height, cornerRadius, tint, textureU0, textureV0, textureU1, textureV1);
    }

    /**
     * Draws a GUI atlas sprite stretched over a rectangle.
     *
     * @param spriteId  sprite id
     * @param positionX local left
     * @param positionY local top
     * @param width     local width
     * @param height    local height
     * @param tintArgb  packed ARGB tint
     */
    public void drawSprite(Identifier spriteId, float positionX, float positionY, float width, float height, int tintArgb) {
        int tint = withAlpha(tintArgb);
        if (width < 1e-3f || height < 1e-3f || isInvisible(tint)) {
            return;
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(positionX, positionY);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteId, 0, 0, Math.max(1, Math.round(width)), Math.max(1, Math.round(height)), tint);
        graphics.pose().popMatrix();
    }

    /**
     * Draws part of a GUI atlas sprite stretched over a rectangle.
     *
     * @param spriteId      sprite id
     * @param textureWidth  width of the full sprite texture
     * @param textureHeight height of the full sprite texture
     * @param sourceX       left of the source region
     * @param sourceY       top of the source region
     * @param positionX     local left
     * @param positionY     local top
     * @param width         local width
     * @param height        local height
     */
    public void drawSprite(Identifier spriteId, int textureWidth, int textureHeight, int sourceX, int sourceY, float positionX, float positionY, float width, float height) {
        if (width < 1e-3f || height < 1e-3f || alpha <= 0f) {
            return;
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(positionX, positionY);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteId, textureWidth, textureHeight, sourceX, sourceY, 0, 0, Math.max(1, Math.round(width)), Math.max(1, Math.round(height)));
        graphics.pose().popMatrix();
    }

    private void drawItemDecorations(ItemStack stack, int originX, int originY) {
        if (stack.isBarVisible()) {
            int left = originX + 2;
            int top = originY + 13;
            drawRect(left, top, 13, 2, ITEM_BAR_BACKGROUND);
            drawRect(left, top, stack.getBarWidth(), 1, ARGB.opaque(stack.getBarColor()));
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        float cooldown = player == null ? 0f : player.getCooldowns().getCooldownPercent(stack, minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true));
        if (cooldown > 0f) {
            int top = originY + Mth.floor(16f * (1f - cooldown));
            drawRect(originX, top, 16, Mth.ceil(16f * cooldown), ITEM_COOLDOWN_OVERLAY);
        }
        if (stack.getCount() != 1) {
            String amount = String.valueOf(stack.getCount());
            drawText(Component.literal(amount), originX + 19 - 2 - minecraft.font.width(amount), originY + 6 + 2, OPAQUE_WHITE, true);
        }
    }

    private AbstractTexture lookupTexture(Identifier texture) {
        AbstractTexture found = TEXTURE_LOOKUP.get(texture);
        if (found == null) {
            found = Minecraft.getInstance().getTextureManager().getTexture(texture);
            TEXTURE_LOOKUP.put(texture, found);
        }
        return found;
    }

    private int withAlpha(int color) {
        return scaleColorAlpha(color, alpha);
    }
}
