package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

/**
 * Manual-layout pane used by the profiles sidebar.
 *
 * <p>Pins a create-profile button at the top, an optional "Edit HUD layout" button at the bottom,
 * and fills the remaining space with a scrollable profile list.
 */
public class FProfilesPaneLayoutWidget extends FWidget {
    private final FWidget topCreateButtonRow;
    private final FWidget profilesScrollClip;
    private final FWidget bottomHudButtonRow;

    public FProfilesPaneLayoutWidget(FWidget topCreateButtonRow, FWidget profilesScrollClip, FWidget bottomHudButtonRow) {
        this.topCreateButtonRow = topCreateButtonRow;
        this.profilesScrollClip = profilesScrollClip;
        this.bottomHudButtonRow = bottomHudButtonRow;
        addChild(topCreateButtonRow);
        addChild(profilesScrollClip);
        if (bottomHudButtonRow != null) {
            addChild(bottomHudButtonRow);
        }
    }

    @Override
    public boolean fillsVerticalInColumn() {
        return true;
    }

    @Override
    public boolean fillsHorizontalInRow() {
        return true;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        float topInset = ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X;
        float bottomInset = ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X;
        float sectionGap = 3f;

        float createButtonHeight = topCreateButtonRow.intrinsicHeightForColumn(measure, layoutWidth);
        topCreateButtonRow.layout(measure, layoutX, layoutY + topInset, layoutWidth, createButtonHeight);
        float reservedTopHeight = topInset + createButtonHeight + sectionGap;

        float reservedBottomHeight = 0f;
        if (bottomHudButtonRow != null) {
            float buttonHeight = bottomHudButtonRow.intrinsicHeightForColumn(measure, layoutWidth);
            float buttonY = layoutY + Math.max(0f, layoutHeight - bottomInset - buttonHeight);
            bottomHudButtonRow.layout(measure, layoutX, buttonY, layoutWidth, buttonHeight);
            reservedBottomHeight = buttonHeight + bottomInset + sectionGap;
        }

        float scrollPad = 2f;
        float scrollHeight = Math.max(0f, layoutHeight - reservedTopHeight - reservedBottomHeight - 2f * scrollPad);
        profilesScrollClip.layout(measure, layoutX, layoutY + reservedTopHeight + scrollPad, layoutWidth, scrollHeight);
    }
}
