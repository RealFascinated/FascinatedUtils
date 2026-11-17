package cc.fascinated.fascinatedutils.gui.core;

import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.layout.MeasuredSize;
import cc.fascinated.fascinatedutils.gui.node.display.TooltipNode;

import java.util.Map;

/**
 * Tooltip layer owned by one {@link UiHost}. The input router reports which node the pointer rests on, and the UI host calls
 * {@link #render} after drawing the whole tree so the tooltip sits above every other node.
 */
public class UiTooltips {
    private static final long HOVER_DELAY_MILLIS = 450L;
    private static final int POINTER_OFFSET = 12;
    private static final int EDGE_MARGIN = 2;

    private final TooltipNode bubble = new TooltipNode();
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
            // Recompose rebuilds nodes, so a new instance with the same text is still the same tooltip.
            boolean sameTooltip = owner != null && tooltipNode != null && tooltipText(owner).equals(tooltipText(tooltipNode));
            tooltipNode = owner;
            if (!sameTooltip) {
                hoverStartMillis = System.currentTimeMillis();
            }
        }
        pointerX = nextPointerX;
        pointerY = nextPointerY;
    }

    private static String tooltipText(UiNode node) {
        String text = node.tooltip().get();
        return text == null ? "" : text;
    }

    /**
     * Follows the tooltip owner to its replacement after recomposition.
     *
     * @param previousToNext mapping from previous nodes to their replacements
     */
    void retarget(Map<UiNode, UiNode> previousToNext) {
        if (tooltipNode != null) {
            tooltipNode = previousToNext.get(tooltipNode);
        }
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
        bubble.setText(text);
        MeasuredSize size = bubble.measure(renderFrame, screenWidth, screenHeight);
        int boxX = Math.max(EDGE_MARGIN, Math.min(Math.round(pointerX) + POINTER_OFFSET, screenWidth - size.width() - EDGE_MARGIN));
        int boxY = Math.round(pointerY) + POINTER_OFFSET;
        if (boxY + size.height() > screenHeight - EDGE_MARGIN) {
            boxY = Math.round(pointerY) - size.height() - EDGE_MARGIN;
        }
        bubble.arrange(renderFrame, boxX, boxY, size.width(), size.height());
        bubble.render(renderFrame, 0f);
    }
}
