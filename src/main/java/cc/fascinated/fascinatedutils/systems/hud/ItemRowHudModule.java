package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;

import java.util.List;

public abstract class ItemRowHudModule extends HudModule {
    protected ItemRowHudModule(String widgetId, String name, float minWidth) {
        super(widgetId, name, minWidth);
    }

    protected abstract List<HudContent.ItemRow> rows(float deltaSeconds);

    @Override
    protected HudContent produceContent(float deltaSeconds, boolean editorMode) {
        List<HudContent.ItemRow> rowList = rows(deltaSeconds);
        if (rowList == null || rowList.isEmpty()) {
            return new HudContent.ItemRows(List.of());
        }
        return new HudContent.ItemRows(rowList);
    }
}
