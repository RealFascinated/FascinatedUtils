package cc.fascinated.fascinatedutils.gui.themes;

import cc.fascinated.fascinatedutils.gui.GuiTheme;
import cc.fascinated.fascinatedutils.gui.theme.UiColor;

/**
 * Default FascinatedUtils GUI theme: Meteor-inspired dark violet-gray palette and layout scalars.
 */
public class FascinatedGuiTheme extends GuiTheme {

    public static final FascinatedGuiTheme INSTANCE = new FascinatedGuiTheme();

    private static final int BACKGROUND = UiColor.argb("#C0101018");
    private static final int SURFACE = UiColor.argb("#80181422");
    private static final int SURFACE_ELEVATED = UiColor.argb("#90242030");
    private static final int BORDER = UiColor.argb("#22ffffff");
    private static final int BORDER_MUTED = UiColor.argb("#15ffffff");
    private static final int BORDER_HOVER = UiColor.argb("#889468f6");
    private static final int MODULE_LIST_ROW = UiColor.argb("#00000000");
    private static final int MODULE_LIST_ROW_HOVER = UiColor.argb("#20ffffff");
    private static final int MODULE_LIST_ROW_SELECTED = UiColor.argb("#30913de2");
    private static final int ACCENT = UiColor.argb("#913de2");
    private static final int ACCENT_BRIGHT = UiColor.argb("#b565f8");
    private static final int TEXT_ACCENT = UiColor.argb("#e9d5ff");
    private static final int TEXT_PRIMARY = UiColor.argb("#f5f5f5");
    private static final int TEXT_MUTED = UiColor.argb("#969696");
    private static final int TEXT_LABEL = UiColor.argb("#6b6680");
    private static final int TOGGLE_ON_FILL = UiColor.argb("#882db84d");
    private static final int TOGGLE_ON_BORDER = UiColor.argb("#8850f070");
    private static final int TOGGLE_ON_FILL_HOVER = UiColor.argb("#992ea043");
    private static final int TOGGLE_OFF_FILL = UiColor.argb("#888b2d3a");
    private static final int TOGGLE_OFF_BORDER = UiColor.argb("#88da3633");
    private static final int TOGGLE_OFF_FILL_HOVER = UiColor.argb("#99a33847");
    private static final int TOGGLE_OFF_BORDER_HOVER = UiColor.argb("#99ff7b72");
    private static final int TOGGLE_ON_SUMMARY = UiColor.argb("#86efac");
    private static final int THUMB = UiColor.argb("#ffffff");
    private static final int HINT_BG = UiColor.argb("#D01a1424");
    private static final int HINT_BORDER = UiColor.argb("#806d28d9");
    private static final int HINT_TEXT = UiColor.argb("#e5e5e5");
    private static final int SECTION_HEADER_BACKGROUND = UiColor.argb("#00000000");
    private static final int SECTION_HEADER_TEXT = UiColor.argb("#8b949e");
    private static final int WIDGET_STATE_ENABLED_FILL = UiColor.argb("#882d8f6e");
    private static final int WIDGET_STATE_ENABLED_FILL_HOVER = UiColor.argb("#9934a67e");
    private static final int WIDGET_STATE_ENABLED_BORDER = UiColor.argb("#887dd4b4");
    private static final int WIDGET_STATE_DISABLED_FILL = UiColor.argb("#888b2d3a");
    private static final int WIDGET_STATE_DISABLED_FILL_HOVER = UiColor.argb("#99a33847");
    private static final int WIDGET_STATE_DISABLED_BORDER = UiColor.argb("#88e08594");
    private static final int WIDGET_STATE_INACTIVE_FILL = UiColor.argb("#40181422");
    private static final int WIDGET_STATE_INACTIVE_FILL_HOVER = UiColor.argb("#55242030");
    private static final int WIDGET_STATE_INACTIVE_BORDER = UiColor.argb("#30ffffff");
    private static final int WIDGET_STATE_LABEL = UiColor.argb("#ffffff");
    private static final int WIDGET_STATE_LABEL_MUTED = UiColor.argb("#9ca3af");
    private static final float CARD_CORNER_RADIUS_PX = 8f;
    private static final float RESET_GLYPH_SIZE_PX = 7f;

    @Override
    public int background() {
        return BACKGROUND;
    }

    @Override
    public int surface() {
        return SURFACE;
    }

    @Override
    public int surfaceElevated() {
        return SURFACE_ELEVATED;
    }

    @Override
    public int border() {
        return BORDER;
    }

    @Override
    public int borderMuted() {
        return BORDER_MUTED;
    }

    @Override
    public int borderHover() {
        return BORDER_HOVER;
    }

    @Override
    public int moduleListRow() {
        return MODULE_LIST_ROW;
    }

    @Override
    public int moduleListRowHover() {
        return MODULE_LIST_ROW_HOVER;
    }

    @Override
    public int moduleListRowSelected() {
        return MODULE_LIST_ROW_SELECTED;
    }

    @Override
    public int accent() {
        return ACCENT;
    }

    @Override
    public int accentBright() {
        return ACCENT_BRIGHT;
    }

    @Override
    public int textPrimary() {
        return TEXT_PRIMARY;
    }

    @Override
    public int textMuted() {
        return TEXT_MUTED;
    }

    @Override
    public int textAccent() {
        return TEXT_ACCENT;
    }

    @Override
    public int textLabel() {
        return TEXT_LABEL;
    }

    @Override
    public int toggleOnFill() {
        return TOGGLE_ON_FILL;
    }

    @Override
    public int toggleOnBorder() {
        return TOGGLE_ON_BORDER;
    }

    @Override
    public int toggleOnFillHover() {
        return TOGGLE_ON_FILL_HOVER;
    }

    @Override
    public int toggleOffFill() {
        return TOGGLE_OFF_FILL;
    }

    @Override
    public int toggleOffBorder() {
        return TOGGLE_OFF_BORDER;
    }

    @Override
    public int toggleOffFillHover() {
        return TOGGLE_OFF_FILL_HOVER;
    }

    @Override
    public int toggleOffBorderHover() {
        return TOGGLE_OFF_BORDER_HOVER;
    }

    @Override
    public int toggleOnSummary() {
        return TOGGLE_ON_SUMMARY;
    }

    @Override
    public int thumb() {
        return THUMB;
    }

    @Override
    public int hintBackground() {
        return HINT_BG;
    }

    @Override
    public int hintBorder() {
        return HINT_BORDER;
    }

    @Override
    public int hintText() {
        return HINT_TEXT;
    }

    @Override
    public int sectionHeaderBackground() {
        return SECTION_HEADER_BACKGROUND;
    }

    @Override
    public int sectionHeaderText() {
        return SECTION_HEADER_TEXT;
    }

    @Override
    public int widgetStateEnabledFill() {
        return WIDGET_STATE_ENABLED_FILL;
    }

    @Override
    public int widgetStateEnabledFillHover() {
        return WIDGET_STATE_ENABLED_FILL_HOVER;
    }

    @Override
    public int widgetStateEnabledBorder() {
        return WIDGET_STATE_ENABLED_BORDER;
    }

    @Override
    public int widgetStateDisabledFill() {
        return WIDGET_STATE_DISABLED_FILL;
    }

    @Override
    public int widgetStateDisabledFillHover() {
        return WIDGET_STATE_DISABLED_FILL_HOVER;
    }

    @Override
    public int widgetStateDisabledBorder() {
        return WIDGET_STATE_DISABLED_BORDER;
    }

    @Override
    public int widgetStateInactiveFill() {
        return WIDGET_STATE_INACTIVE_FILL;
    }

    @Override
    public int widgetStateInactiveFillHover() {
        return WIDGET_STATE_INACTIVE_FILL_HOVER;
    }

    @Override
    public int widgetStateInactiveBorder() {
        return WIDGET_STATE_INACTIVE_BORDER;
    }

    @Override
    public int widgetStateLabel() {
        return WIDGET_STATE_LABEL;
    }

    @Override
    public int widgetStateLabelMuted() {
        return WIDGET_STATE_LABEL_MUTED;
    }

    @Override
    public float cardCornerRadius() {
        return CARD_CORNER_RADIUS_PX;
    }

    @Override
    public float resetGlyphSize() {
        return RESET_GLYPH_SIZE_PX;
    }
}
