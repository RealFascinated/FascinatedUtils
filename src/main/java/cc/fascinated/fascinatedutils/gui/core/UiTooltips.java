package cc.fascinated.fascinatedutils.gui.core;

import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.render.UiText;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;

/**
 * Shared tooltip layer. The input router reports which node the pointer rests on, and the UI host calls
 * {@link #render} after drawing the whole tree so the tooltip sits above every other node.
 */
public class UiTooltips {
    public static final UiTooltips INSTANCE = new UiTooltips();

    private static final long HOVER_DELAY_MILLIS = 450L;
    private static final int PADDING = 4;
    private static final int POINTER_OFFSET = 12;
    private static final int EDGE_MARGIN = 2;

    private UiNode tooltipNode;
    private float pointerX;
    private float pointerY;
    private long hoverStartMillis;

    /**
     * Records the node under the pointer, restarting the hover delay when the tooltip owner changes.
     *
     * @param hoverLeaf the deepest node under the pointer, or null when none
     * @param nextPointerX pointer x position
     * @param nextPointerY pointer y position
     */
    public void pointerMoved(UiNode hoverLeaf, float nextPointerX, float nextPointerY) {
        UiNode owner = null;
        for (UiNode cursor = hoverLeaf; cursor != null; cursor = cursor.parent()) {
            if (cursor.tooltip() != null) {
                owner = cursor;
                break;
            }
        }
        if (owner != tooltipNode) {
            tooltipNode = owner;
            hoverStartMillis = System.currentTimeMillis();
        }
        pointerX = nextPointerX;
        pointerY = nextPointerY;
    }

    /**
     * Hides the current tooltip and restarts the hover delay, for example after a click.
     */
    public void reset() {
        tooltipNode = null;
    }

    /**
     * Draws the pending tooltip, if its owner has been hovered long enough.
     *
     * @param renderFrame the active renderer
     * @param screenWidth logical width available for the tooltip
     * @param screenHeight logical height available for the tooltip
     */
    public void render(AlumiteGuiRenderer renderFrame, int screenWidth, int screenHeight) {
        if (tooltipNode == null || !tooltipNode.visible() || System.currentTimeMillis() - hoverStartMillis < HOVER_DELAY_MILLIS) {
            return;
        }
        String text = tooltipNode.tooltip().get();
        if (text == null || text.isBlank()) {
            return;
        }
        UiTheme theme = renderFrame.theme();
        int textWidth = renderFrame.measureTextWidth(text, false);
        int boxWidth = textWidth + PADDING * 2;
        int boxHeight = renderFrame.fontHeight() + PADDING * 2;
        int boxX = Math.max(EDGE_MARGIN, Math.min(Math.round(pointerX) + POINTER_OFFSET, screenWidth - boxWidth - EDGE_MARGIN));
        int boxY = Math.round(pointerY) + POINTER_OFFSET;
        if (boxY + boxHeight > screenHeight - EDGE_MARGIN) {
            boxY = Math.round(pointerY) - boxHeight - EDGE_MARGIN;
        }
        renderFrame.flushText();
        renderFrame.drawRoundedRectFrame(boxX, boxY, boxWidth, boxHeight, UiMetrics.RADIUS, theme.borderSubtle(), theme.surfaceRaised(), 1);
        UiText.of(text).color(theme.textPrimary()).draw(renderFrame, boxX + PADDING, boxY + PADDING);
        renderFrame.flushText();
    }
}
