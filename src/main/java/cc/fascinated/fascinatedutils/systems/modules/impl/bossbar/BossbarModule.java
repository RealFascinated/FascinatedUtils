package cc.fascinated.fascinatedutils.systems.modules.impl.bossbar;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.HudWidgetAppearanceBuilders;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HUDWidgetAnchor;
import cc.fascinated.fascinatedutils.systems.modules.impl.bossbar.hud.BossbarHudPanel;

public class BossbarModule extends HudHostModule {

    public static final float BOSS_BAR_WIDTH = 182f;

    private final BooleanSetting hideBar = BooleanSetting.builder().id("hide_bar").defaultValue(false).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final BooleanSetting showBackground = HudWidgetAppearanceBuilders.showBackground().defaultValue(false).build();
    private final ColorSetting backgroundColor = HudWidgetAppearanceBuilders.backgroundColor().defaultValue(SettingColor.fromArgb(0x55000000)).build();
    private final BooleanSetting roundedCorners = HudWidgetAppearanceBuilders.roundedCorners().build();
    private final SliderSetting roundingRadius = HudWidgetAppearanceBuilders.roundingRadius().build();
    private final BooleanSetting showBorder = HudWidgetAppearanceBuilders.showBorder().build();
    private final SliderSetting borderThickness = HudWidgetAppearanceBuilders.borderThickness().build();
    private final ColorSetting borderColor = HudWidgetAppearanceBuilders.borderColor().build();

    public BossbarModule() {
        super("bossbar", "Bossbar", HudDefaults.builder()
            .defaultState(true)
            .defaultAnchor(HUDWidgetAnchor.TOP)
            .defaultXOffset(0)
            .defaultYOffset(5)
            .defaultPadding(0f)
            .build());
        addSetting(hideBar);
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
        registerHudPanel(new BossbarHudPanel(this));
    }

    public boolean bossbarHudHideBarGraphic() {
        return hideBar.isEnabled();
    }
}
