package cc.fascinated.fascinatedutils.systems.hud.widgets;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudMiniMessageModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class TextHudPanel extends HudMiniMessageModule {
    private final Function<Float, List<String>> contentProvider;
    private final Function<Float, List<String>> editorPreviewProvider;

    private TextHudPanel(String widgetId, String displayName, float minWidth, Function<Float, List<String>> contentProvider, Function<Float, List<String>> editorPreviewProvider) {
        super(widgetId, displayName, minWidth);
        this.contentProvider = contentProvider;
        this.editorPreviewProvider = editorPreviewProvider;
        registerHudPanel(new MiniMessageHudPanel(this, widgetId, minWidth));
    }

    public static Builder create(String widgetId, String displayName, float minWidth) {
        return new Builder(widgetId, displayName, minWidth);
    }

    @Override
    protected List<String> lines(float deltaSeconds) {
        return contentProvider.apply(deltaSeconds);
    }

    @Override
    protected List<String> resolveRawMiniMessageLines(float deltaSeconds, boolean editorMode) {
        if (editorMode && editorPreviewProvider != null) {
            return editorPreviewProvider.apply(deltaSeconds);
        }
        return lines(deltaSeconds);
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

            TextHudPanel panel = new TextHudPanel(widgetId, displayName, minWidth, contentProvider, editorPreviewProvider);
            for (BooleanSetting setting : settings) {
                panel.addSetting(setting);
            }
            return panel;
        }
    }
}
