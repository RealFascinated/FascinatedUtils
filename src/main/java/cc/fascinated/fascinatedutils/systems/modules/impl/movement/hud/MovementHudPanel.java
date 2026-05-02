package cc.fascinated.fascinatedutils.systems.modules.impl.movement.hud;

import cc.fascinated.fascinatedutils.systems.modules.impl.movement.MovementModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;

import java.util.List;

public class MovementHudPanel extends MiniMessageHudPanel {

    public MovementHudPanel(MovementModule movementModule) {
        super(movementModule, "movement", 0f);
    }

    @Override
    protected List<String> computeMiniMessageLines(float deltaSeconds, boolean editorMode) {
        MovementModule movementHost = (MovementModule) hudHostModule();
        return movementHost.resolveMovementHudLines(editorMode);
    }
}
