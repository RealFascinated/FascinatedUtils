package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import org.jspecify.annotations.Nullable;

public class MiniMessageHudPanel extends HudPanel {
    private final HudMiniMessageModule hostModule;
    private final float baseMinWidthValue;

    public MiniMessageHudPanel(HudMiniMessageModule hostModule, String panelId, float baseMinWidth) {
        super(hostModule, panelId, baseMinWidth);
        this.hostModule = hostModule;
        this.baseMinWidthValue = baseMinWidth;
    }

    @Override
    protected float effectiveMinWidth() {
        return hostModule.resolvePanelMinWidth(baseMinWidthValue);
    }

    @Override
    protected HudContent produceHudContent(float deltaSeconds, boolean editorMode) {
        return new HudContent.TextLines(hostModule.miniMessageLinesWithCache(deltaSeconds, editorMode));
    }

    @Override
    protected HudAnchorContentAlignment.@Nullable Horizontal hudTextLineHorizontalAlignmentOverride() {
        return hostModule.hostTextLineHorizontalAlignment();
    }
}
