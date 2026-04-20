package cc.fascinated.fascinatedutils.gui.widgets.settings;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.widgets.FColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FSpacerWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import org.jspecify.annotations.Nullable;

/**
 * Composite widget that wraps a {@link FBooleanSettingRowWidget} and an expandable sub-panel containing child settings.
 *
 * <p>A chevron button is wired to the boolean row; clicking it shows or hides the sub-panel below the row. Sub-settings
 * are passed from {@link BooleanSetting#getSubSettings()}.
 */
public class FBooleanWithSubSettingsWidget extends FWidget {

    private static final float SUB_PANEL_TOP_GAP = 2f;
    private static final float SUB_PANEL_INSET = 16f;

    private final FBooleanSettingRowWidget booleanRow;
    private final FColumnWidget subPanel;
    private final float booleanRowHeight;
    private boolean expanded = false;

    /**
     * Factory that creates an editor widget for a single setting in the sub-panel.
     */
    @FunctionalInterface
    public interface SubSettingEditorFactory {

        /**
         * Creates a row editor for the given sub-setting.
         *
         * @param setting              the sub-setting to edit
         * @param innerWidth           available row width inside the sub-panel
         * @param sliderValueColumnStartX value-column alignment offset
         * @return editor widget, or {@code null} to skip
         */
        @Nullable FWidget create(Setting<?> setting, float innerWidth, float sliderValueColumnStartX);
    }

    public FBooleanWithSubSettingsWidget(BooleanSetting booleanSetting, Runnable onPersist, float outerWidth, float rowHeight, float valueColumnStartX, SubSettingEditorFactory subEditorFactory) {
        this.booleanRowHeight = rowHeight;

        booleanRow = new FBooleanSettingRowWidget(booleanSetting, outerWidth, rowHeight, onPersist, valueColumnStartX);
        booleanRow.setChevronHandlers(() -> expanded, () -> expanded = !expanded);
        addChild(booleanRow);

        float subPanelWidth = Math.max(0f, outerWidth - 2f * SUB_PANEL_INSET);
        float subValueColumnStartX = Math.max(0f, valueColumnStartX - SUB_PANEL_INSET);
        subPanel = new FColumnWidget(SettingsUiMetrics.SETTING_GROUP_GAP, Align.START);
        for (Setting<?> subSetting : booleanSetting.getSubSettings()) {
            FWidget editor = subEditorFactory.create(subSetting, subPanelWidth, subValueColumnStartX);
            if (editor != null) {
                subPanel.addChild(editor);
            }
        }
        addChild(subPanel);
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        if (!expanded || subPanel.childrenView().isEmpty()) {
            return booleanRowHeight;
        }
        return booleanRowHeight + SUB_PANEL_TOP_GAP + subPanel.intrinsicHeightForColumn(measure, Math.max(0f, widthBudget - 2f * SUB_PANEL_INSET));
    }

    @Override
    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        return booleanRow.intrinsicWidthForRow(measure, heightBudget);
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        booleanRow.layout(measure, layoutX, layoutY, layoutWidth, booleanRowHeight);

        boolean showSubPanel = expanded && !subPanel.childrenView().isEmpty();
        subPanel.setVisible(showSubPanel);
        if (showSubPanel) {
            float subX = layoutX + SUB_PANEL_INSET;
            float subY = layoutY + booleanRowHeight + SUB_PANEL_TOP_GAP;
            float subWidth = Math.max(0f, layoutWidth - 2f * SUB_PANEL_INSET);
            float subHeight = Math.max(0f, layoutHeight - booleanRowHeight - SUB_PANEL_TOP_GAP);
            subPanel.layout(measure, subX, subY, subWidth, subHeight);
        }
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        // children render themselves via FWidget.render()
    }
}
