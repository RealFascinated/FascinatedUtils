package cc.fascinated.fascinatedutils.systems.hud.widgets;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.HudWidgetAppearanceBuilders;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class TextHudPanel extends HudHostModule {
    private final Function<Float, List<String>> contentProvider;
    private final Function<Float, List<String>> editorPreviewProvider;

    private final BooleanSetting showBackground = HudWidgetAppearanceBuilders.showBackground().build();
    private final BooleanSetting roundedCorners = HudWidgetAppearanceBuilders.roundedCorners().build();
    private final SliderSetting roundingRadius = HudWidgetAppearanceBuilders.roundingRadius().build();
    private final BooleanSetting showBorder = HudWidgetAppearanceBuilders.showBorder().build();
    private final SliderSetting borderThickness = HudWidgetAppearanceBuilders.borderThickness().build();
    private final ColorSetting backgroundColor = HudWidgetAppearanceBuilders.backgroundColor().build();
    private final ColorSetting borderColor = HudWidgetAppearanceBuilders.borderColor().build();
    private final BooleanSetting removeMinimumWidth = HudWidgetAppearanceBuilders.removeMinimumWidth().build();
    private final SliderSetting padding = HudWidgetAppearanceBuilders.padding().build();
    private final BooleanSetting textShadow = HudWidgetAppearanceBuilders.textShadow().build();

    private TextHudPanel(String widgetId, String displayName, float minWidth, Function<Float, List<String>> contentProvider, Function<Float, List<String>> editorPreviewProvider) {
        super(widgetId, displayName, HudDefaults.builder().build());
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
        addSetting(removeMinimumWidth);
        addSetting(padding);
        addSetting(textShadow);
        this.contentProvider = contentProvider;
        this.editorPreviewProvider = editorPreviewProvider;
        registerHudPanel(new MiniMessageHudPanel(this, widgetId, minWidth) {
            @Override
            protected List<String> computeMiniMessageLines(float deltaSeconds, boolean editorMode) {
                if (editorMode && editorPreviewProvider != null) {
                    return editorPreviewProvider.apply(deltaSeconds);
                }
                return contentProvider.apply(deltaSeconds);
            }
        });
    }

    public static Builder create(String widgetId, String displayName, float minWidth) {
        return new Builder(widgetId, displayName, minWidth);
    }

    public static class Builder {
        private final String widgetId;
        private final String displayName;
        private final float minWidth;
        private final List<BooleanSetting> settings = new ArrayList<>();
        private Function<Float, List<String>> contentProvider;
        private Function<Float, List<String>> editorPreviewProvider;

        public Builder(String widgetId, String displayName, float minWidth) {
            this.widgetId = widgetId;
            this.displayName = displayName;
            this.minWidth = minWidth;
        }

        public Builder lines(Function<Float, List<String>> provider) {
            this.contentProvider = provider;
            return this;
        }

        public Builder editorPreview(Function<Float, List<String>> provider) {
            this.editorPreviewProvider = provider;
            return this;
        }

        public Builder setting(BooleanSetting setting) {
            this.settings.add(setting);
            return this;
        }

        public TextHudPanel build() {
            if (contentProvider == null) {
                throw new IllegalStateException("contentProvider must be set via .lines()");
            }
            if (editorPreviewProvider == null) {
                editorPreviewProvider = delta -> List.of("Preview");
            }

            TextHudPanel hudModule = new TextHudPanel(widgetId, displayName, minWidth, contentProvider, editorPreviewProvider);
            for (BooleanSetting setting : settings) {
                hudModule.addSetting(setting);
            }
            return hudModule;
        }
    }
}
