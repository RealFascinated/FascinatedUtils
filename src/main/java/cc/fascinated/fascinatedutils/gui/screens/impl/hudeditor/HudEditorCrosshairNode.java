package cc.fascinated.fascinatedutils.gui.screens.impl.hudeditor;

import cc.fascinated.fascinatedutils.gui.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiColor;

/**
 * Faint dashed center crosshair so the screen center is visible while dragging a panel.
 */
public class HudEditorCrosshairNode extends PositionedNode<HudEditorCrosshairNode> {

    private static final int COLOR = UiColor.argb("#28913de2");
    private static final float DASH = 4f;
    private static final float GAP = 4f;

    public HudEditorCrosshairNode() {
        full();
    }

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        float totalWidth = bounds().width();
        float totalHeight = bounds().height();
        float centerX = (float) Math.floor(totalWidth * 0.5f);
        float centerY = (float) Math.floor(totalHeight * 0.5f);
        boolean draw = true;
        for (float cursor = 0f; cursor < totalHeight; ) {
            float segment = Math.min(draw ? DASH : GAP, totalHeight - cursor);
            if (draw) {
                renderFrame.drawRect(centerX, cursor, 1f, segment, COLOR);
            }
            cursor += segment;
            draw = !draw;
        }
        draw = true;
        for (float cursor = 0f; cursor < totalWidth; ) {
            float segment = Math.min(draw ? DASH : GAP, totalWidth - cursor);
            if (draw) {
                renderFrame.drawRect(cursor, centerY, segment, 1f, COLOR);
            }
            cursor += segment;
            draw = !draw;
        }
    }
}
