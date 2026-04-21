package cc.fascinated.fascinatedutils.gui.theme;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SettingsUiMetrics {

    public static final float SETTING_ROW_PADDING_X = 8f;
    public static final float SETTING_ROW_PADDING_Y = 1f;
    public static final float SETTING_GROUP_GAP = 2f;
    public static final float CATEGORY_SECTION_GAP = 2f;
    /**
     * Vertical gap between a category title row and the first indented setting row beneath it.
     */
    public static final float CATEGORY_AFTER_HEADER_ROW_GAP = 2f;
    /**
     * Extra horizontal inset from the shell inner edge for module/widget settings detail chrome (search bars, empty
     * states, headers outside the setting-row grid).
     */
    public static final float SETTINGS_DETAIL_CONTENT_INSET_X_DESIGN = 10f;
    /**
     * Leading shell margin for module setting category title rows only (logical design px).
     */
    public static final float SETTINGS_DETAIL_CATEGORY_TITLE_LEFT_INSET_X_DESIGN = 8f;
    /**
     * Leading shell margin for categorized and uncategorized setting rows (logical design px).
     */
    public static final float SETTINGS_DETAIL_CATEGORY_CONTENT_LEFT_INSET_X_DESIGN = 6f;
    /**
     * Trailing shell margin for module setting category headers and category rows (logical design px): thirty
     * percent of {@link #SETTINGS_DETAIL_CONTENT_INSET_X_DESIGN} so value controls align nearer the viewport edge.
     */
    public static final float SETTINGS_DETAIL_CATEGORY_CONTENT_RIGHT_INSET_X_DESIGN = 2.4f;
    public static final float SHELL_CONTROL_HEIGHT_DESIGN = 17f;

    public static final float BOOLEAN_TOGGLE_OUTER_W = 26f;
    public static final float BOOLEAN_TOGGLE_OUTER_H = 13f;
    public static final float SLIDER_VALUE_COL_W = 28f;
    public static final float SETTING_VALUE_CONTROL_GAP = 6f;
    /**
     * Horizontal gap between the two columns in a grouped boolean settings grid (design px).
     */
    public static final float BOOLEAN_GRID_COLUMN_GAP_DESIGN = 8f;
    /**
     * Vertical gap between rows inside a two-column boolean block (design px).
     */
    public static final float BOOLEAN_GRID_ROW_GAP_DESIGN = 2f;
    private static final float FLOAT_TRACK_SLOT_H = 14f;

    /**
     * Inner width of a setting row body from the width passed into the editor (shell padding subtracted).
     *
     * @param widthProp row width in layout space
     * @return inner width for label and controls
     */
    public static float settingInnerBodyWidth(float widthProp) {
        return Math.max(0f, widthProp - 2f * SETTING_ROW_PADDING_X);
    }

    public static float floatTitleValueRowHeight() {
        float titleLine = ModSettingsTheme.shellDesignBodyLineHeight();
        return Math.max(BOOLEAN_TOGGLE_OUTER_H, titleLine);
    }

    public static float booleanInnerHeight() {
        return Math.max(ModSettingsTheme.shellDesignBodyLineHeight(), BOOLEAN_TOGGLE_OUTER_H);
    }

    public static float booleanOuterHeight() {
        return booleanInnerHeight() + 2f * SETTING_ROW_PADDING_Y;
    }

    public static float floatInnerHeight() {
        return Math.max(floatTitleValueRowHeight(), FLOAT_TRACK_SLOT_H);
    }

    public static float floatOuterHeight() {
        return floatInnerHeight() + 2f * SETTING_ROW_PADDING_Y;
    }
}
