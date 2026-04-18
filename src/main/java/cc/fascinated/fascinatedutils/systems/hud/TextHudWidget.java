package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class TextHudWidget extends HudMiniMessageModule {
    private final Function<Float, List<String>> contentProvider;
    private final Function<Float, List<String>> editorPreviewProvider;

    private TextHudWidget(String widgetId, String displayName, float minWidth, Function<Float, List<String>> contentProvider, Function<Float, List<String>> editorPreviewProvider) {
        super(widgetId, displayName, minWidth);
        this.contentProvider = contentProvider;
        this.editorPreviewProvider = editorPreviewProvider;
    }

    public static Builder create(String widgetId, String displayName, float minWidth) {
        return new Builder(widgetId, displayName, minWidth);
    }

    @Override
    protected List<String> lines(float deltaSeconds) {
        return contentProvider.apply(deltaSeconds);
    }

    @Override
    protected HudContent produceContent(float deltaSeconds, boolean editorMode) {
        List<String> rawLines = editorMode && editorPreviewProvider != null ? editorPreviewProvider.apply(deltaSeconds) : lines(deltaSeconds);
        if (rawLines == null || rawLines.isEmpty()) {
            rawLines = List.of("");
        }
        return new HudContent.TextLines(rawLines);
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

        public TextHudWidget build() {
            if (contentProvider == null) {
                throw new IllegalStateException("contentProvider must be set via .lines()");
            }
            if (editorPreviewProvider == null) {
                editorPreviewProvider = delta -> List.of("Preview");
            }

            TextHudWidget widget = new TextHudWidget(widgetId, displayName, minWidth, contentProvider, editorPreviewProvider);
            for (BooleanSetting setting : settings) {
                widget.addSetting(setting);
            }
            return widget;
        }
    }
}
