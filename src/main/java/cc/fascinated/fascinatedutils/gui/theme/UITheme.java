package cc.fascinated.fascinatedutils.gui.theme;

public class UITheme {
    public static final int COLOR_BACKGROUND = UiColor.argb("#C0101018");
    /**
     * Slightly cooler bottom stop for vertical shell fills (see mod settings overlay).
     */
    public static final int COLOR_BACKGROUND_GRADIENT_END = UiColor.argb("#C00e0c1c");
    public static final int COLOR_SURFACE = UiColor.argb("#80181422");
    public static final int COLOR_SURFACE_HOVER = UiColor.argb("#90242030");
    public static final int COLOR_BORDER = UiColor.argb("#22ffffff");
    public static final int COLOR_BORDER_SUBTLE = UiColor.argb("#15ffffff");
    public static final int COLOR_BORDER_FOCUS = UiColor.argb("#889468f6");
    /**
     * Meteor-style GUI accent ({@code rgb(145,61,226)}).
     */
    public static final int COLOR_ACCENT = UiColor.argb("#913de2");
    /**
     * Brighter violet for hover emphasis (tabs, close control).
     */
    public static final int COLOR_ACCENT_HOVER = UiColor.argb("#a855f7");
    public static final int COLOR_ACCENT_DIM = UiColor.argb("#66913de2");
    public static final int COLOR_TEXT_PRIMARY = UiColor.argb("#f5f5f5");
    public static final int COLOR_TEXT_SECONDARY = UiColor.argb("#969696");
    public static final int COLOR_TEXT_DISABLED = UiColor.argb("#555566");
    public static final int COLOR_TEXT_ACCENT = UiColor.argb("#e9d5ff");
    /**
     * HUD panel fill: same idea as vanilla chat line background (black with default text-background opacity, ~25%).
     */
    public static final int COLOR_HUD_BACKGROUND = UiColor.argb("#55000000");

    /**
     * Optional HUD panel outline: cooler violet-gray, higher opacity than {@link #COLOR_BORDER_SUBTLE} so it stays
     * readable on top of {@link #COLOR_HUD_BACKGROUND} and the world.
     */
    public static final int COLOR_HUD_PANEL_BORDER = UiColor.argb("#30ffffff");

    public static final int COLOR_SCROLLBAR_TRACK = UiColor.argb("#20ffffff");
    public static final int COLOR_SCROLLBAR_THUMB = UiColor.argb("#607080a8");
    public static final int COLOR_CLEAR = UiColor.argb("#00000000");

    /**
     * Shadow plate behind HUD panel (ARGB).
     */
    public static final int PANEL_SHADOW = UiColor.argb("#50000000");
    public static final float PANEL_SHADOW_PAD_PX = 2f;

    /**
     * Single-line cap height (logical px) used only when no {@link net.minecraft.client.Minecraft} exists yet;
     * live layout should use {@link net.minecraft.client.gui.Font#lineHeight}.
     */
    public static final float BASE_FONT_HEIGHT = 9f;
    /**
     * Default stroke for bordered rects (logical px).
     */
    public static final float BORDER_THICKNESS_PX = 1f;

    public static final float TOGGLE_HEIGHT = 16f;
    public static final float SLIDER_HEIGHT = 28f;
    public static final float BUTTON_HEIGHT = 18f;
    public static final float INPUT_HEIGHT = 28f;

    /**
     * Extra horizontal scissor slack on vertical scroll clips (logical px) so flush rounded cards and trailing row
     * controls are not shaved at the viewport edge.
     */
    public static final float SCROLL_CLIP_HORIZONTAL_OUTSET_LOGICAL = 8f;
    /**
     * Total width reserved on the right of a vertical scroll clip; content stays left of this strip.
     */
    public static final float SCROLLBAR_RESERVED_W = 6f;
    /**
     * Visible scrollbar thumb width, centred in {@link #SCROLLBAR_RESERVED_W}.
     */
    public static final float SCROLLBAR_THUMB_W = 3f;
    /**
     * Minimum vertical thumb height regardless of content ratio.
     */
    public static final float SCROLLBAR_MIN_THUMB_H = 16f;

    public static final float TOGGLE_ANIM_SPEED = 22f;
    public static final float SLIDER_EPS = 1e-4f;
    public static final float SCROLL_WHEEL_SCALE = 28f;

    public static final float INPUT_PAD_X = 6f;
    public static final float INPUT_PAD_Y = 4f;
    public static final float INPUT_LABEL_GAP = 3f;

    public static final float SLIDER_TRACK_H = 7f;

    public static final float PADDING_XS = 3f;
    public static final float PADDING_SM = 6f;
    public static final float PADDING_MD = 10f;
    public static final float PADDING_LG = 16f;

    public static final float GAP_XS = 3f;
    public static final float GAP_SM = 4f;
    public static final float GAP_MD = 8f;
    public static final float GAP_LG = 14f;
    public static final float GAP_XL = 20f;
}
