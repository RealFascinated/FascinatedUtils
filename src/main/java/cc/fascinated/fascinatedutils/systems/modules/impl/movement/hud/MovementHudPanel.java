package cc.fascinated.fascinatedutils.systems.modules.impl.movement.hud;

import cc.fascinated.fascinatedutils.systems.modules.impl.movement.MovementModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;

public class MovementHudPanel extends MiniMessageHudPanel {

    public MovementHudPanel(MovementModule movementModule) {
        super(movementModule, "movement", 0f);
    }
}
