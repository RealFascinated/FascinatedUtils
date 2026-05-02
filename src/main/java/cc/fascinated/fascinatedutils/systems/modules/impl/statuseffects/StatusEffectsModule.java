package cc.fascinated.fascinatedutils.systems.modules.impl.statuseffects;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.modules.impl.statuseffects.hud.StatusEffectsHudPanel;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.HudWidgetAppearanceBuilders;

public class StatusEffectsModule extends HudHostModule {

    private final BooleanSetting showAmplifier = BooleanSetting.builder().id("show_amplifier").defaultValue(true).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final BooleanSetting showDuration = BooleanSetting.builder().id("show_duration").defaultValue(true).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final SliderSetting flashTimeWhenEnding = SliderSetting.builder().id("flash_time_when_ending").defaultValue(10f).minValue(0f).maxValue(30f).step(1f).valueFormatter(value -> {
        int seconds = Math.round(value.floatValue());
        return seconds <= 0 ? "Off" : seconds + "s";
    }).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final EnumSetting<SortMode> sortMode = EnumSetting.<SortMode>builder().id("sort_mode").defaultValue(SortMode.REMAINING_TIME).valueFormatter(SortMode::label).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final EnumSetting<DisplayMode> displayMode = EnumSetting.<DisplayMode>builder().id("display_mode").defaultValue(DisplayMode.DETAILED).valueFormatter(DisplayMode::label).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final BooleanSetting showBackground = HudWidgetAppearanceBuilders.showBackground().build();
    private final BooleanSetting roundedCorners = HudWidgetAppearanceBuilders.roundedCorners().build();
    private final SliderSetting roundingRadius = HudWidgetAppearanceBuilders.roundingRadius().build();
    private final BooleanSetting showBorder = HudWidgetAppearanceBuilders.showBorder().build();
    private final SliderSetting borderThickness = HudWidgetAppearanceBuilders.borderThickness().build();
    private final ColorSetting backgroundColor = HudWidgetAppearanceBuilders.backgroundColor().build();
    private final ColorSetting borderColor = HudWidgetAppearanceBuilders.borderColor().build();

    public StatusEffectsModule() {
        super("status_effects", "Status Effects", HudDefaults.builder().build());
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
        addSetting(showAmplifier);
        addSetting(showDuration);
        addSetting(flashTimeWhenEnding);
        addSetting(sortMode);
        addSetting(displayMode);
        registerHudPanel(new StatusEffectsHudPanel(this));
    }

    public boolean effectsShowAmplifier() {
        return showAmplifier.isEnabled();
    }

    public boolean effectsShowDurationSetting() {
        return showDuration.isEnabled();
    }

    public float effectsFlashEndingSeconds() {
        return flashTimeWhenEnding.getValue().floatValue();
    }

    public SortMode effectsSortMode() {
        return sortMode.getValue();
    }

    public DisplayMode effectsDisplayMode() {
        return displayMode.getValue();
    }

    public enum SortMode {
        REMAINING_TIME("Remaining Time"), ALPHABETICAL("Alphabetical");

        private final String label;

        SortMode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public enum DisplayMode {
        DETAILED("Detailed"), COMPACT("Compact");

        private final String label;

        DisplayMode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
