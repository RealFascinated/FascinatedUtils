package cc.fascinated.fascinatedutils.gui;

import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.renderer.text.TextRenderer;
import cc.fascinated.fascinatedutils.renderer.text.VanillaTextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * GUI theme: palette tokens (ARGB colors and small design-space scalars) and text rendering policy (Meteor
 * {@code GuiTheme} analogue). Call sites use {@link #textPrimary()}, {@link #surface()}, and siblings on the active
 * theme instance (for example {@link FascinatedGuiTheme#INSTANCE}).
 */
public abstract class GuiTheme {

    public abstract int background();

    public abstract int surface();

    public abstract int surfaceElevated();

    public abstract int border();

    public abstract int borderMuted();

    public abstract int borderHover();

    public abstract int moduleListRow();

    public abstract int moduleListRowHover();

    public abstract int moduleListRowSelected();

    public abstract int accent();

    public abstract int accentBright();

    public abstract int textPrimary();

    public abstract int textMuted();

    public abstract int textAccent();

    public abstract int textLabel();

    public abstract int toggleOnFill();

    public abstract int toggleOnBorder();

    public abstract int toggleOnFillHover();

    public abstract int toggleOffFill();

    public abstract int toggleOffBorder();

    public abstract int toggleOffFillHover();

    public abstract int toggleOffBorderHover();

    public abstract int toggleOnSummary();

    public abstract int thumb();

    public abstract int hintBackground();

    public abstract int hintBorder();

    public abstract int hintText();

    public abstract int sectionHeaderBackground();

    public abstract int sectionHeaderText();

    public abstract int widgetStateEnabledFill();

    public abstract int widgetStateEnabledFillHover();

    public abstract int widgetStateEnabledBorder();

    public abstract int widgetStateDisabledFill();

    public abstract int widgetStateDisabledFillHover();

    public abstract int widgetStateDisabledBorder();

    public abstract int widgetStateInactiveFill();

    public abstract int widgetStateInactiveFillHover();

    public abstract int widgetStateInactiveBorder();

    public abstract int widgetStateLabel();

    public abstract int widgetStateLabelMuted();

    public abstract float cardCornerRadius();

    public abstract float resetGlyphSize();

    /**
     * Text renderer used for the remainder of the current frame while this theme is active.
     *
     * @param drawContext active draw context
     * @return non-null renderer implementation
     */
    public TextRenderer textRenderer(GuiGraphicsExtractor drawContext) {
        return VanillaTextRenderer.INSTANCE;
    }
}
