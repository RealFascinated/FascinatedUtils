package cc.fascinated.fascinatedutils.gui.theme;

import cc.fascinated.fascinatedutils.gui.GuiDesignSpace;
import net.minecraft.client.Minecraft;

public class ModSettingsTheme {
    /**
     * Max shell width before clamping by canvas width times {@link #SHELL_MAX_WIDTH_FRAC}.
     */
    public static final float PANEL_MAX_W = 720f;
    /**
     * Max shell height before clamping by canvas height times {@link #SHELL_MAX_HEIGHT_FRAC}.
     */
    public static final float PANEL_MAX_H = 420f;
    public static final float SHELL_MAX_WIDTH_FRAC = 0.98f;
    public static final float SHELL_MAX_HEIGHT_FRAC = 0.82f;
    public static final float PANEL_ASPECT_W = 16f;
    public static final float PANEL_ASPECT_H = 9f;

    /**
     * Scrim over blurred world; lighter than full dim now that the shell uses a GPU blur pass.
     */
    public static final int SCRIM = UiColor.argb("#44000000");
    /**
     * Outer rounded border for the mod settings window shell.
     */
    public static final int SHELL_BORDER = UiColor.argb("#30ffffff");
    /**
     * Horizontal padding from the split line to sidebar and settings column content.
     */
    public static final float SIDEBAR_SEPARATOR_PAD_X = 10f;

    public static final float TOPBAR_HEIGHT = 34f;
    /**
     * Square title-bar controls (close, tab track, HUD layout chip) use this fraction of the scaled {@link #TOPBAR_HEIGHT}.
     */
    public static final float TITLEBAR_CLOSE_BUTTON_HEIGHT_FRAC = 0.58f;
    /**
     * Corner fillet radius for the mod settings window shell (shadow, gradient panel, title bar); logical px, scaled
     * with {@link GuiDesignSpace#pxUniform(float)} when drawing.
     */
    public static final float SHELL_CORNER_RADIUS = 8f;
    /**
     * Maximum width of the segmented tab control before clamping to the body width (logical px).
     */
    public static final float SHELL_TAB_STRIP_MAX_WIDTH = 300f;
    /**
     * Horizontal inset from the shell body edge when centering the tab strip (logical px).
     */
    public static final float SHELL_TAB_STRIP_MARGIN_X = 6f;
    /**
     * Horizontal gap between adjacent shell tab segments (Modules / Widgets / Settings), design logical px.
     */
    public static final float SHELL_TAB_STRIP_SEGMENT_GAP = 6f;
    /**
     * Corner radius for module/widget segment buttons (logical px).
     */
    public static final float SHELL_TAB_STRIP_SEGMENT_CORNER_RADIUS = 3f;

    /**
     * Side length of the square close button and height of tab segments / HUD layout chip in the mod settings title bar.
     * Call only while {@link GuiDesignSpace} is active (for example during {@code ModSettingsScreen} layout).
     *
     * @return size in logical shell pixels, at least 1
     */
    public static float titleBarSquareControlSizePx() {
        float topBarHeightPx = GuiDesignSpace.pxY(TOPBAR_HEIGHT);
        return Math.max(1f, Math.round(topBarHeightPx * TITLEBAR_CLOSE_BUTTON_HEIGHT_FRAC));
    }

    /**
     * Logical body line height for mod-settings rows; matches the active {@link net.minecraft.client.gui.Font}
     * when a client exists.
     *
     * @return line height in logical pixels, at least 1
     */
    public static float shellDesignBodyLineHeight() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return Math.max(1f, UITheme.BASE_FONT_HEIGHT);
        }
        return Math.max(1f, client.font.lineHeight);
    }
}
