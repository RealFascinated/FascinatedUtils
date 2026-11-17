package cc.fascinated.fascinatedutils.gui.screens.impl.hudeditor;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudPanel;

/**
 * Corner handle on the selected HUD panel; dragging it rescales the panel.
 */
public class HudScaleHandleNode extends PositionedNode<HudScaleHandleNode> {

    private final HudPanel panel;
    private final HudEditorPointerSession session;

    public HudScaleHandleNode(HudPanel panel, HudEditorPointerSession session) {
        this.panel = panel;
        this.session = session;
    }

    @Override
    public void layout(AlumiteGuiRenderer renderFrame, int positionX, int positionY, int width, int height) {
        int handleSize = Math.round(HudEditorChrome.scaleHandleSize());
        bounds().set(Math.round(HudEditorChrome.scaleHandleLeft(panel)), Math.round(HudEditorChrome.scaleHandleTop(panel)), handleSize, handleSize);
    }

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        renderFrame.drawRect(bounds().positionX(), bounds().positionY(), bounds().width(), bounds().height(), HudEditorChrome.SCALE_HANDLE_FILL);
        renderFrame.drawBorder(bounds().positionX(), bounds().positionY(), bounds().width(), bounds().height(), 1f, HudEditorChrome.SELECTED_COLOR);
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
        session.beginScale(panel, pointerX, pointerY);
        return true;
    }

    @Override
    public boolean onPointerMove(float pointerX, float pointerY) {
        if (session.scalingWidget() != panel) {
            return false;
        }
        session.scaleTo(pointerX, pointerY);
        return true;
    }

    @Override
    public boolean onPointerRelease(float pointerX, float pointerY, int button) {
        if (button != 0 || session.scalingWidget() != panel) {
            return false;
        }
        session.endInteraction();
        return true;
    }
}
