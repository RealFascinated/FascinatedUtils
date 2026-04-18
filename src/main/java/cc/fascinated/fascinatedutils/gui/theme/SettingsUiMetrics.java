package cc.fascinated.fascinatedutils.gui.theme;

import cc.fascinated.fascinatedutils.gui.GuiDesignSpace;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;

@UtilityClass
public class SettingsUiMetrics {

    public static final float SETTING_ROW_PADDING_X = 12f;
    public static final float SETTING_ROW_PADDING_Y = 2f;
    public static final float SETTING_GROUP_GAP = UITheme.GAP_XS;
    public static final float CATEGORY_SECTION_GAP = SETTING_GROUP_GAP;
    /**
     * Logical px: indent module settings rows under a category header (paired with left-aligned category title).
     */
    public static final float MODULE_SETTING_CATEGORY_INDENT_X_DESIGN = 12f;
    /**
     * Extra horizontal inset from the shell inner edge for module/widget settings detail rows and category headers.
     */
    public static final float SETTINGS_DETAIL_CONTENT_INSET_X_DESIGN = 12f;
    public static final float SHELL_CONTROL_HEIGHT_DESIGN = 24f;

    public static final float BOOLEAN_TOGGLE_OUTER_W = 28f;
    public static final float BOOLEAN_TOGGLE_OUTER_H = 14f;
    public static final float SLIDER_VALUE_COL_W = 40f;
    public static final float SETTING_VALUE_CONTROL_GAP = UITheme.GAP_MD;
    private static final float FLOAT_TRACK_SLOT_H = 20f;

    /**
     * Inner width of a setting row body from the width passed into the editor (shell padding subtracted).
     *
     * @param widthProp row width in layout space
     * @return inner width for label and controls
     */
    public static float settingInnerBodyWidth(float widthProp) {
        return Math.max(0f, widthProp - 2f * GuiDesignSpace.pxX(SETTING_ROW_PADDING_X));
    }

    public static float floatTitleValueRowHeight() {
        Minecraft client = Minecraft.getInstance();
        float titleLine = client != null ? GuiDesignSpace.pxY(client.font.lineHeight) : GuiDesignSpace.pxY(ModSettingsTheme.shellDesignBodyLineHeight());
        return Math.max(GuiDesignSpace.pxUniform(BOOLEAN_TOGGLE_OUTER_H), titleLine);
    }

    public static float booleanInnerHeight() {
        return Math.max(GuiDesignSpace.pxY(ModSettingsTheme.shellDesignBodyLineHeight()), GuiDesignSpace.pxUniform(BOOLEAN_TOGGLE_OUTER_H));
    }

    public static float booleanOuterHeight() {
        return booleanInnerHeight() + 2f * GuiDesignSpace.pxY(SETTING_ROW_PADDING_Y);
    }

    public static float floatInnerHeight() {
        return Math.max(floatTitleValueRowHeight(), GuiDesignSpace.pxY(FLOAT_TRACK_SLOT_H));
    }

    public static float floatOuterHeight() {
        return floatInnerHeight() + 2f * GuiDesignSpace.pxY(SETTING_ROW_PADDING_Y);
    }
}
