package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;

import java.util.List;

public abstract class ItemRowHudModule extends HudModule {
    private final BooleanSetting showBackground = BooleanSetting.builder().id(SETTING_SHOW_BACKGROUND).defaultValue(true).translationKeyPath("fascinatedutils.module.show_hud_background").categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final BooleanSetting showBorder = BooleanSetting.builder().id(SETTING_SHOW_BORDER).defaultValue(false).translationKeyPath("fascinatedutils.module.show_border").categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final SliderSetting borderThickness = SliderSetting.builder().id(SETTING_BORDER_THICKNESS).defaultValue(2f).minValue(1f).maxValue(3f).step(1f).translationKeyPath("fascinatedutils.module.border_thickness").categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final SliderSetting padding = SliderSetting.builder().id(SETTING_PADDING).defaultValue(6f).minValue(0f).maxValue(16f).step(1f).translationKeyPath("fascinatedutils.module.padding").categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    protected ItemRowHudModule(String widgetId, String name, float minWidth) {
        super(widgetId, name, minWidth);
        addSetting(showBackground);
        addSetting(showBorder);
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
