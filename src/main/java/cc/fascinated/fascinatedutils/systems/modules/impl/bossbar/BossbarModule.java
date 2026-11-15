package cc.fascinated.fascinatedutils.systems.modules.impl.bossbar;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HUDWidgetAnchor;
import cc.fascinated.fascinatedutils.systems.modules.impl.bossbar.hud.BossbarHudPanel;

public class BossbarModule extends HudHostModule {

    public static final float BOSS_BAR_WIDTH = 182f;

    private final BooleanSetting hideBar = BooleanSetting.builder().id("hide_bar").defaultValue(false).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    public BossbarModule() {
        super("bossbar", "Bossbar", HudDefaults.builder().defaultState(true).defaultAnchor(HUDWidgetAnchor.TOP).defaultXOffset(0).defaultYOffset(5).build());
        addSetting(hideBar);
        addAppearanceSettings(false);
        registerHudPanel(new BossbarHudPanel(this));
    }

    @Override
    protected boolean appearanceDefaultShowBackground() {
        return false;
    }

    @Override
    protected float appearanceDefaultPadding() {
        return 0f;
    }

    public boolean bossbarHudHideBarGraphic() {
        return hideBar.isEnabled();
    }
}
