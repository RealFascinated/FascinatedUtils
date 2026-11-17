package cc.fascinated.fascinatedutils.gui.screens.impl.settings;

import cc.fascinated.fascinatedutils.gui.core.UiScale;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import lombok.Getter;

/**
 * Window and content geometry of the settings shell for the current screen size.
 */
@Getter
public class SettingsLayout {

    public static final int WINDOW_PADDING = UiMetrics.SPACE_SM;
    public static final int SIDEBAR_WIDTH = 150;
    public static final int GAP = UiMetrics.SPACE_SM;
    public static final int CONTROL_HEIGHT = UiMetrics.CONTROL_HEIGHT;

    private static final int MAX_WIDTH = 940;
    private static final int MAX_HEIGHT = 580;
    private static final int MIN_WIDTH = 560;
    private static final int MIN_HEIGHT = 320;
    private static final int SCREEN_MARGIN = 20;
    private static final int MIN_TILE_WIDTH = 150;
    private static final int WIDE_GRID_THRESHOLD = 4;
    private static final int SCROLLBAR_RESERVE = 12;

    private final int windowWidth;
    private final int windowHeight;
    private final int mainWidth;
    private final int contentHeight;
    private final int gridColumns;
    private final int tileWidth;

    /**
     * Computes the geometry from the current screen size.
     */
    public SettingsLayout() {
        windowWidth = (int) Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, UiScale.screenWidth() - 2 * SCREEN_MARGIN));
        windowHeight = (int) Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, UiScale.screenHeight() - 2 * SCREEN_MARGIN));
        mainWidth = windowWidth - 3 * WINDOW_PADDING - SIDEBAR_WIDTH;
        contentHeight = windowHeight - 2 * WINDOW_PADDING - CONTROL_HEIGHT - GAP;
        int baseColumns = Math.max(2, (mainWidth - SCROLLBAR_RESERVE + GAP) / (MIN_TILE_WIDTH + GAP));
        gridColumns = baseColumns >= WIDE_GRID_THRESHOLD ? baseColumns + 1 : baseColumns;
        tileWidth = (mainWidth - SCROLLBAR_RESERVE - (gridColumns - 1) * GAP) / gridColumns;
    }
}
