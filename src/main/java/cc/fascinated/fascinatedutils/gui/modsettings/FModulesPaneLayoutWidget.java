package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

/**
 * Manual-layout pane used by the modules card grid.
 *
 * <p>Pins a controls/filter row at the top and fills the remaining space with a scrollable card grid.
 */
public class FModulesPaneLayoutWidget extends FWidget {
    private final FWidget controlsRowHost;
    private final FWidget modulesScrollClip;

    public FModulesPaneLayoutWidget(FWidget controlsRowHost, FWidget modulesScrollClip) {
        this.controlsRowHost = controlsRowHost;
        this.modulesScrollClip = modulesScrollClip;
        addChild(controlsRowHost);
        addChild(modulesScrollClip);
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
        float bottomInset = 4f;
        float sectionGap = ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X;

        float controlsHeight = controlsRowHost.intrinsicHeightForColumn(measure, layoutWidth);
        float controlsY = layoutY + topInset;
        controlsRowHost.layout(measure, layoutX, controlsY, layoutWidth, controlsHeight);

        float scrollY = controlsY + controlsHeight + sectionGap;
        float scrollHeight = Math.max(0f, layoutY + layoutHeight - bottomInset - scrollY);
        modulesScrollClip.layout(measure, layoutX, scrollY, layoutWidth, scrollHeight);
    }
}
