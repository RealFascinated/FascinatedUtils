package cc.fascinated.fascinatedutils.systems.hud;

import java.util.List;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;

public abstract class ItemRowHudModule extends HudModule {
    private final BooleanSetting showBackground = HudWidgetAppearanceBuilders.showBackground().build();
    private final BooleanSetting roundedCorners = HudWidgetAppearanceBuilders.roundedCorners().build();
    private final BooleanSetting showBorder = HudWidgetAppearanceBuilders.showBorder().build();
    private final SliderSetting roundingRadius = HudWidgetAppearanceBuilders.roundingRadius().build();
    private final SliderSetting borderThickness = HudWidgetAppearanceBuilders.borderThickness().build();
    private final SliderSetting padding = HudWidgetAppearanceBuilders.padding().build();

    protected ItemRowHudModule(String widgetId, String name, float minWidth) {
        super(widgetId, name, minWidth);
        addSetting(showBackground);
        addSetting(roundedCorners);
        addSetting(showBorder);
        addSetting(roundingRadius);
        addSetting(borderThickness);
        addSetting(padding);
    }

    protected abstract List<HudContent.ItemRow> rows(float deltaSeconds);

    @Override
    protected HudContent produceContent(float deltaSeconds, boolean editorMode) {
        List<HudContent.ItemRow> rowList = rows(deltaSeconds);
        if (rowList == null || rowList.isEmpty()) {
            return new HudContent.ItemRows(List.of());
        }
        return new HudContent.ItemRows(rowList);
    }
}
