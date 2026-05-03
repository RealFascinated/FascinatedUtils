package cc.fascinated.fascinatedutils.systems.modules.impl.coordinates;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.HudWidgetAppearanceBuilders;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HUDWidgetAnchor;
import cc.fascinated.fascinatedutils.systems.modules.impl.coordinates.hud.CoordinatesHudPanel;

public class CoordinatesWidget extends HudHostModule {

    public enum CoordinatesLayout {
        VERTICAL, HORIZONTAL
    }

    private final EnumSetting<CoordinatesLayout> layout = EnumSetting.<CoordinatesLayout>builder()
            .id("layout")
            .defaultValue(CoordinatesLayout.VERTICAL)
            .valueFormatter(coordsLayout -> coordsLayout == CoordinatesLayout.VERTICAL ? "Vertical" : "Horizontal")
            .categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY)
            .build();
    private final SliderSetting blockPrecision = SliderSetting.builder().id("block_precision")
            .defaultValue(0f).minValue(0f).maxValue(3f).step(1f).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final BooleanSetting showBackground = HudWidgetAppearanceBuilders.showBackground().build();
    private final BooleanSetting roundedCorners = HudWidgetAppearanceBuilders.roundedCorners().build();
    private final SliderSetting roundingRadius = HudWidgetAppearanceBuilders.roundingRadius().build();
    private final BooleanSetting showBorder = HudWidgetAppearanceBuilders.showBorder().build();
    private final SliderSetting borderThickness = HudWidgetAppearanceBuilders.borderThickness().build();
    private final ColorSetting backgroundColor = HudWidgetAppearanceBuilders.backgroundColor().build();
    private final ColorSetting borderColor = HudWidgetAppearanceBuilders.borderColor().build();

    public CoordinatesWidget() {
        super("coordinates", "Coordinates", HudDefaults.builder().defaultState(true).defaultAnchor(HUDWidgetAnchor.TOP_LEFT).defaultXOffset(5).defaultYOffset(5).build());
        addSetting(layout);
        addSetting(blockPrecision);
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
        registerHudPanel(new CoordinatesHudPanel(this));
    }

    public CoordinatesLayout coordinatesHudLayout() {
        return layout.getValue();
    }

    public float coordinatesHudBlockDecimals() {
        return blockPrecision.getValue().floatValue();
    }
}
