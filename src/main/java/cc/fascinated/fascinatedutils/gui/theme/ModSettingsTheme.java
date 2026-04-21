package cc.fascinated.fascinatedutils.gui.theme;


public class ModSettingsTheme {
    /**
     * Max shell width before clamping by canvas width times {@link #SHELL_MAX_WIDTH_FRAC}.
     */
    public static final float PANEL_MAX_W = 550f;
    /**
     * Max shell height before clamping by canvas height times {@link #SHELL_MAX_HEIGHT_FRAC}.
     */
    public static final float PANEL_MAX_H = 335f;
    public static final float SHELL_MAX_WIDTH_FRAC = 0.98f;
    public static final float SHELL_MAX_HEIGHT_FRAC = 0.82f;
    public static final float PANEL_ASPECT_W = 16f;
    public static final float PANEL_ASPECT_H = 10f;

    /**
     * Scrim over blurred world; lighter than full dim now that the shell uses a GPU blur pass.
     */
    public static final int SCRIM = UiColor.argb("#44000000");
    /**
     * Outer rounded border for the mod settings window shell.
     */
    public static final int SHELL_BORDER = UiColor.argb("#12ffffff");
    /**
     * Horizontal padding from the split line to sidebar and settings column content.
     */
    public static final float SIDEBAR_SEPARATOR_PAD_X = 9f;

    public static final float TOPBAR_HEIGHT = 28f;
    /**
     * Square title-bar controls (close, tab track, HUD layout chip) use this fraction of the scaled {@link #TOPBAR_HEIGHT}.
     */
    public static final float TITLEBAR_CLOSE_BUTTON_HEIGHT_FRAC = 0.58f;
    /**
     * Corner fillet radius for the mod settings window shell (shadow, gradient panel, title bar); logical px, scaled
     * .
     */
    public static final float SHELL_CORNER_RADIUS = 10f;
    /**
     * Maximum width of the segmented tab control before clamping to the body width (logical px).
     */
    public static final float SHELL_TAB_STRIP_MAX_WIDTH = 210f;
    /**
     * Horizontal inset from the shell body edge when centering the tab strip (logical px).
     */
    public static final float SHELL_TAB_STRIP_MARGIN_X = 4f;
    /**
     * Horizontal gap between adjacent shell tab segments (Modules / Widgets / Settings), design logical px.
     */
    public static final float SHELL_TAB_STRIP_SEGMENT_GAP = 4f;
    /**
     * Corner radius for module/widget segment buttons (logical px).
     */
    public static final float SHELL_TAB_STRIP_SEGMENT_CORNER_RADIUS = 6f;

    /**
     * Design-space body line height for mod settings layout (labels, row sizing); slightly below vanilla font line
     * height so the shell reads smaller without a global GUI scale factor.
     */
    public static final float SHELL_BODY_LINE_HEIGHT_DESIGN = 6f;

    /**
     * Side length of the square close button and height of tab segments / HUD layout chip in the mod settings title bar.
     * Call only while {@link GuiDesignSpace} is active (for example during {@code ModSettingsScreen} layout).
     *
     * @return size in logical shell pixels, at least 1
     */
    public static float titleBarSquareControlSizePx() {
        float topBarHeightPx = TOPBAR_HEIGHT;
        return Math.max(1f, Math.round(topBarHeightPx * TITLEBAR_CLOSE_BUTTON_HEIGHT_FRAC));
    }

    /**
     * Logical body line height for mod-settings rows and related layout.
     *
     * @return line height in logical pixels, at least 1
     */
    public static float shellDesignBodyLineHeight() {
        return Math.max(1f, SHELL_BODY_LINE_HEIGHT_DESIGN);
    }
}
