package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;

import java.util.List;

public abstract class ItemRowHudModule extends HudHostModule {
    private final BooleanSetting showBackground = HudWidgetAppearanceBuilders.showBackground().build();
    private final BooleanSetting roundedCorners = HudWidgetAppearanceBuilders.roundedCorners().build();
    private final BooleanSetting showBorder = HudWidgetAppearanceBuilders.showBorder().build();
    private final SliderSetting roundingRadius = HudWidgetAppearanceBuilders.roundingRadius().build();
    private final SliderSetting borderThickness = HudWidgetAppearanceBuilders.borderThickness().build();
    private final ColorSetting backgroundColor = HudWidgetAppearanceBuilders.backgroundColor().build();
    private final ColorSetting borderColor = HudWidgetAppearanceBuilders.borderColor().build();

    protected ItemRowHudModule(String widgetId, String name, float minWidth) {
        super(widgetId, name, HudDefaults.builder().build());
        addSetting(showBackground);
        addSetting(roundedCorners);
        addSetting(showBorder);
        addSetting(roundingRadius);
        addSetting(borderThickness);
        addSetting(backgroundColor);
        addSetting(borderColor);
        showBackground.addSubSetting(backgroundColor);
        roundedCorners.addSubSetting(roundingRadius);
        showBorder.addSubSetting(borderThickness);
        showBorder.addSubSetting(borderColor);
    }

    protected abstract List<HudContent.ItemRow> rows(float deltaSeconds);

    /**
     * Rows for this frame; override when editor preview should differ from {@link #rows}.
     *
     * @param deltaSeconds frame delta in seconds
     * @param editorMode   true in the HUD layout editor
     * @return item rows to render
     */
    protected List<HudContent.ItemRow> panelRows(float deltaSeconds, boolean editorMode) {
        return rows(deltaSeconds);
    }
}
