package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;

import java.util.List;

public class ItemRowHostHudPanel extends HudPanel {

    private final ItemRowHudModule hostModule;

    public ItemRowHostHudPanel(ItemRowHudModule hostModule, String panelId, float minWidth) {
        super(hostModule, panelId, minWidth);
        this.hostModule = hostModule;
    }

    @Override
    protected HudContent produceHudContent(float deltaSeconds, boolean editorMode) {
        List<HudContent.ItemRow> rowList = hostModule.panelRows(deltaSeconds, editorMode);
        if (rowList == null || rowList.isEmpty()) {
            return new HudContent.ItemRows(List.of());
        }
        return new HudContent.ItemRows(rowList);
    }
}
