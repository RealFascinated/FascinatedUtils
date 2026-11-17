package cc.fascinated.fascinatedutils.gui.screens.impl.hudeditor;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;

/**
 * Full-screen node behind the HUD panels; pressing empty space clears the selection.
 */
public class HudEditorBackdropNode extends PositionedNode<HudEditorBackdropNode> {

    private final HudEditorPointerSession session;

    public HudEditorBackdropNode(HudEditorPointerSession session) {
        this.session = session;
        full();
    }

    @Override
    public boolean hitTarget() {
        return true;
    }

    @Override
    public boolean onPointerPress(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        session.clearSelection();
        return true;
    }
}
