package cc.fascinated.fascinatedutils.systems.modules.impl.debug.hud;

import cc.fascinated.fascinatedutils.systems.modules.impl.debug.DebugWidget;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;

public class DebugHudPanel extends MiniMessageHudPanel {

    private static final float DEBUG_HUD_PANEL_MIN_WIDTH = 200f;

    public DebugHudPanel(DebugWidget debugWidget) {
        super(debugWidget, "debug", DEBUG_HUD_PANEL_MIN_WIDTH);
    }
}
