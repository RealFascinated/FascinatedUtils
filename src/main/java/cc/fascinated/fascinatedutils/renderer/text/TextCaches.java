package cc.fascinated.fascinatedutils.renderer.text;

import cc.fascinated.fascinatedutils.common.collection.LRUCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * Bounded caches for text measuring and conversion, used on the render thread only. Widths and visual-order sequences
 * are kept per {@link TextVariant}; everything that depends on the font is dropped when the font instance or a font
 * option ({@code forceUnicodeFont}, {@code japaneseGlyphVariants}) changes.
 */
public class TextCaches {
    private static final int CACHE_LIMIT = 2048;
    private final LRUCache<String, Integer>[] widths = newCaches();
    private final LRUCache<String, FormattedCharSequence>[] sequences = newCaches();
    private final LRUCache<String, Integer> miniMessageWidths = new LRUCache<>(CACHE_LIMIT);
    private final LRUCache<String, FormattedCharSequence> miniMessageSequences = new LRUCache<>(CACHE_LIMIT);
    private final LRUCache<String, Component> miniMessageComponents = new LRUCache<>(CACHE_LIMIT);
    @Nullable
    private Font cachedFont;
    private int cachedFontOptionsKey;

    @SuppressWarnings("unchecked")
    private static <V> LRUCache<String, V>[] newCaches() {
        LRUCache<String, V>[] caches = new LRUCache[TextVariant.values().length];
        for (int index = 0; index < caches.length; index++) {
            caches[index] = new LRUCache<>(CACHE_LIMIT);
        }
        return caches;
    }

    /**
     * Looks up a measured width.
     *
     * @param variant the style the text is measured in
     * @param text    the text
     * @return the cached width, or null when not measured since the last font change
     */
    @Nullable
    public Integer width(TextVariant variant, String text) {
        validate();
        return widths[variant.ordinal()].get(text);
    }

    /**
     * Stores a measured width.
     *
     * @param variant the style the text was measured in
     * @param text    the text
     * @param width   the measured width
     */
    public void putWidth(TextVariant variant, String text, int width) {
        widths[variant.ordinal()].put(text, width);
    }

    /**
     * Returns the styled visual-order sequence for a literal string, building it on first use.
     *
     * @param variant the style to apply
     * @param text    the literal text
     * @return the sequence ready for drawing
     */
    public FormattedCharSequence literal(TextVariant variant, String text) {
        LRUCache<String, FormattedCharSequence> cache = sequences[variant.ordinal()];
        FormattedCharSequence sequence = cache.get(text);
        if (sequence == null) {
            sequence = FormattedCharSequence.forward(text, variant.style());
            cache.put(text, sequence);
        }
        return sequence;
    }

    /**
     * @param miniMessage MiniMessage source
     * @return the parsed component, or null when not parsed yet
     */
    @Nullable
    public Component miniMessageComponent(String miniMessage) {
        return miniMessageComponents.get(miniMessage);
    }

    /**
     * Stores a parsed component.
     *
     * @param miniMessage MiniMessage source
     * @param component   the parsed (or literal fallback) component
     */
    public void putMiniMessageComponent(String miniMessage, Component component) {
        miniMessageComponents.put(miniMessage, component);
    }

    /**
     * @param miniMessage MiniMessage source
     * @return the cached visual-order sequence, or null
     */
    @Nullable
    public FormattedCharSequence miniMessageSequence(String miniMessage) {
        return miniMessageSequences.get(miniMessage);
    }

    /**
     * Stores a visual-order sequence.
     *
     * @param miniMessage MiniMessage source
     * @param sequence    the sequence
     */
    public void putMiniMessageSequence(String miniMessage, FormattedCharSequence sequence) {
        miniMessageSequences.put(miniMessage, sequence);
    }

    /**
     * @param miniMessage MiniMessage source
     * @return the cached width, or null when not measured since the last font change
     */
    @Nullable
    public Integer miniMessageWidth(String miniMessage) {
        validate();
        return miniMessageWidths.get(miniMessage);
    }

    /**
     * Stores a MiniMessage width.
     *
     * @param miniMessage MiniMessage source
     * @param width       the measured width
     */
    public void putMiniMessageWidth(String miniMessage, int width) {
        miniMessageWidths.put(miniMessage, width);
    }

    /**
     * Drops every cached width and sequence, for example after a language or resource reload.
     */
    public void clear() {
        for (LRUCache<String, Integer> cache : widths) {
            cache.clear();
        }
        for (LRUCache<String, FormattedCharSequence> cache : sequences) {
            cache.clear();
        }
        miniMessageWidths.clear();
        miniMessageSequences.clear();
    }

    private void validate() {
        Minecraft minecraft = Minecraft.getInstance();
        Options options = minecraft.options;
        int fontOptionsKey = (options.forceUnicodeFont().get() ? 1 : 0) | (options.japaneseGlyphVariants().get() ? 2 : 0);
        if (minecraft.font != cachedFont || fontOptionsKey != cachedFontOptionsKey) {
            cachedFont = minecraft.font;
            cachedFontOptionsKey = fontOptionsKey;
            clear();
        }
    }
}
