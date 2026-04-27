package cc.fascinated.fascinatedutils.systems.hud.widgets;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.hud.ItemRowHudModule;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ItemRowHudWidget extends ItemRowHudModule {
    private final Function<Float, List<HudContent.ItemRow>> contentProvider;
    private final Function<Float, List<HudContent.ItemRow>> editorPreviewProvider;

    private ItemRowHudWidget(String widgetId, String displayName, float minWidth, Function<Float, List<HudContent.ItemRow>> contentProvider, Function<Float, List<HudContent.ItemRow>> editorPreviewProvider) {
        super(widgetId, displayName, minWidth);
        this.contentProvider = contentProvider;
        this.editorPreviewProvider = editorPreviewProvider;
    }

    public static Builder create(String widgetId, String displayName, float minWidth) {
        return new Builder(widgetId, displayName, minWidth);
    }

    @Override
    protected List<HudContent.ItemRow> rows(float deltaSeconds) {
        return contentProvider.apply(deltaSeconds);
    }

    @Override
    protected HudContent produceContent(float deltaSeconds, boolean editorMode) {
        List<HudContent.ItemRow> rawRows = editorMode && editorPreviewProvider != null ? editorPreviewProvider.apply(deltaSeconds) : rows(deltaSeconds);
        if (rawRows == null || rawRows.isEmpty()) {
            return new HudContent.ItemRows(List.of());
        }
        return new HudContent.ItemRows(rawRows);
    }

    public static class Builder {
        private final String widgetId;
        private final String displayName;
        private final float minWidth;
        private final List<BooleanSetting> settings = new ArrayList<>();
        private Function<Float, List<HudContent.ItemRow>> contentProvider;
        private Function<Float, List<HudContent.ItemRow>> editorPreviewProvider;

        public Builder(String widgetId, String displayName, float minWidth) {
            this.widgetId = widgetId;
            this.displayName = displayName;
            this.minWidth = minWidth;
        }

        public Builder rows(Function<Float, List<HudContent.ItemRow>> provider) {
            this.contentProvider = provider;
            return this;
        }

        public Builder editorPreview(Function<Float, List<HudContent.ItemRow>> provider) {
            this.editorPreviewProvider = provider;
            return this;
        }

        public Builder setting(BooleanSetting setting) {
            this.settings.add(setting);
            return this;
        }

        public ItemRowHudWidget build() {
            if (contentProvider == null) {
                throw new IllegalStateException("contentProvider must be set via .rows()");
            }
            if (editorPreviewProvider == null) {
                editorPreviewProvider = delta -> List.of(new HudContent.ItemRow(net.minecraft.world.item.ItemStack.EMPTY, "Preview"));
            }

            ItemRowHudWidget widget = new ItemRowHudWidget(widgetId, displayName, minWidth, contentProvider, editorPreviewProvider);
            for (BooleanSetting setting : settings) {
                widget.addSetting(setting);
            }
            return widget;
        }
    }
}
