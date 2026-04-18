package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;

import java.util.List;

public abstract class HudMiniMessageModule extends HudModule {
    protected HudMiniMessageModule(String widgetId, String name, float minWidth) {
        super(widgetId, name, minWidth);
    }

    protected abstract List<String> lines(float deltaSeconds);

    @Override
    protected HudContent produceContent(float deltaSeconds, boolean editorMode) {
        List<String> rawLines = lines(deltaSeconds);
        if (rawLines == null || rawLines.isEmpty()) {
            rawLines = List.of("");
        }
        return new HudContent.TextLines(rawLines);
    }
}
