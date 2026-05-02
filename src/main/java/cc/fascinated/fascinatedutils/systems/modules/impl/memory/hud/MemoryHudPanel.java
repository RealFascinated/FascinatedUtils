package cc.fascinated.fascinatedutils.systems.modules.impl.memory.hud;

import cc.fascinated.fascinatedutils.systems.modules.impl.memory.MemoryWidget;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;

public class MemoryHudPanel extends MiniMessageHudPanel {

    public MemoryHudPanel(MemoryWidget memoryWidget) {
        super(memoryWidget, "process_memory", HudHostModule.UTILITY_WIDGET_MIN_WIDTH);
    }
}
