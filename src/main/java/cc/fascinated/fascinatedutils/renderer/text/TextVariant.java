package cc.fascinated.fascinatedutils.renderer.text;

import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * The four ways a literal UI string can be styled: vanilla font or the mod's GUI font, regular or bold. Text caches are
 * keyed by this instead of one cache per flag combination.
 */
public enum TextVariant {
    PLAIN(Style.EMPTY),
    BOLD(Style.EMPTY.withBold(true)),
    GUI_REGULAR(Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("alumite", "inter_regular")))),
    GUI_BOLD(Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("alumite", "inter_bold"))));

    private final Style style;

    TextVariant(Style style) {
        this.style = style;
    }

    /**
     * Picks the variant for a font choice and weight.
     *
     * @param guiFont whether the mod's GUI font is used
     * @param bold    whether the text is bold
     * @return the matching variant
     */
    public static TextVariant of(boolean guiFont, boolean bold) {
        if (guiFont) {
            return bold ? GUI_BOLD : GUI_REGULAR;
        }
        return bold ? BOLD : PLAIN;
    }

    /**
     * @return the style applied to literal strings of this variant
     */
    public Style style() {
        return style;
    }
}
