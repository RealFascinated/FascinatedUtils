package cc.fascinated.fascinatedutils.systems.modules.impl.statuseffects;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.statuseffects.hud.StatusEffectsHudPanel;
import lombok.AllArgsConstructor;

public class StatusEffectsModule extends HudHostModule {

    private final BooleanSetting showAmplifier = BooleanSetting.builder().id("show_amplifier").defaultValue(true).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final BooleanSetting showDuration = BooleanSetting.builder().id("show_duration").defaultValue(true).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final SliderSetting flashTimeWhenEnding = SliderSetting.builder().id("flash_time_when_ending").defaultValue(10f).minValue(0f).maxValue(30f).step(1f).valueFormatter(value -> {
        int seconds = Math.round(value.floatValue());
        return seconds <= 0 ? "Off" : seconds + "s";
    }).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final EnumSetting<SortMode> sortMode = EnumSetting.<SortMode>builder().id("sort_mode").defaultValue(SortMode.REMAINING_TIME).valueFormatter(SortMode::label).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final EnumSetting<DisplayMode> displayMode = EnumSetting.<DisplayMode>builder().id("display_mode").defaultValue(DisplayMode.DETAILED).valueFormatter(DisplayMode::label).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    public StatusEffectsModule() {
        super("status_effects", "Status Effects", HudDefaults.builder().build());
        addAppearanceSettings(false);
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

    @AllArgsConstructor
    public enum SortMode {
        REMAINING_TIME("Remaining Time"), ALPHABETICAL("Alphabetical");

        private final String label;

        public String label() {
            return label;
        }
    }

    @AllArgsConstructor
    public enum DisplayMode {
        DETAILED("Detailed"), COMPACT("Compact");

        private final String label;

        public String label() {
            return label;
        }
    }
}
