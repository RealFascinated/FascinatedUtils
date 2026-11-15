package cc.fascinated.fascinatedutils.systems.modules.impl.coordinates;

import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HUDWidgetAnchor;
import cc.fascinated.fascinatedutils.systems.modules.impl.coordinates.hud.CoordinatesHudPanel;

public class CoordinatesWidget extends HudHostModule {

    private final EnumSetting<CoordinatesLayout> layout = EnumSetting.<CoordinatesLayout>builder().id("layout").defaultValue(CoordinatesLayout.VERTICAL).valueFormatter(coordsLayout -> coordsLayout == CoordinatesLayout.VERTICAL ? "Vertical" : "Horizontal").categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final SliderSetting blockPrecision = SliderSetting.builder().id("block_precision").defaultValue(0f).minValue(0f).maxValue(3f).step(1f).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    public CoordinatesWidget() {
        super("coordinates", "Coordinates", HudDefaults.builder().defaultState(true).defaultAnchor(HUDWidgetAnchor.TOP_LEFT).defaultXOffset(5).defaultYOffset(5).build());
        addSetting(layout);
        addSetting(blockPrecision);
        addAppearanceSettings(false);
        registerHudPanel(new CoordinatesHudPanel(this));
    }

    public CoordinatesLayout coordinatesHudLayout() {
        return layout.getValue();
    }

    public float coordinatesHudBlockDecimals() {
        return blockPrecision.getValue().floatValue();
    }

    public enum CoordinatesLayout {
        VERTICAL, HORIZONTAL
    }
}
