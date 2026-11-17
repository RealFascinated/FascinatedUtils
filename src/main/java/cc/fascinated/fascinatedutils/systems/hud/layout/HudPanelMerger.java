package cc.fascinated.fascinatedutils.systems.hud.layout;

import cc.fascinated.fascinatedutils.systems.hud.panel.HudPanel;

import cc.fascinated.fascinatedutils.systems.hud.ModuleHudState;

import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;

import cc.fascinated.fascinatedutils.gui.render.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.systems.settings.Settings;

import java.util.List;

public class HudPanelMerger {
    private static final float EDGE_EPSILON = 0.6f;
    private static final float SCALE_EPSILON = 0.001f;

    private boolean lastMerging;
    private long lastSignature;

    /**
     * Finds HUD panels whose edges touch and records, per panel, which corners must be square so the touching
     * panels read as one continuous element. The pairwise search is skipped while no panel moved, resized,
     * appeared or disappeared since the last search.
     *
     * @param panels  panels drawn this frame, already anchored
     * @param enabled false to only clear previous merge state (for example in the HUD editor)
     */
    public void update(List<HudPanel> panels, boolean enabled) {
        boolean merging = enabled && Settings.INSTANCE.getSeamlessPanels().isEnabled();
        if (!merging) {
            for (HudPanel panel : panels) {
                panel.getHudState().setSquaredCornerMask(RectCornerRoundMask.NONE);
            }
            lastMerging = false;
            return;
        }
        long signature = layoutSignature(panels);
        if (lastMerging && signature == lastSignature) {
            return;
        }
        lastMerging = true;
        lastSignature = signature;
        for (HudPanel panel : panels) {
            panel.getHudState().setSquaredCornerMask(RectCornerRoundMask.NONE);
        }
        for (HudPanel first : panels) {
            if (!canMerge(first)) {
                continue;
            }
            for (HudPanel second : panels) {
                if (first == second || !canMerge(second)) {
                    continue;
                }
                if (Math.abs(first.getHudState().getScale() - second.getHudState().getScale()) > SCALE_EPSILON) {
                    continue;
                }
                mergeStackedVertically(first, second);
                mergeStackedHorizontally(first, second);
            }
        }
    }

    /**
     * Hashes everything the merge result depends on: which panels are drawn, where, how big, and whether they draw chrome.
     */
    private long layoutSignature(List<HudPanel> panels) {
        long signature = panels.size();
        for (HudPanel panel : panels) {
            ModuleHudState state = panel.getHudState();
            signature = signature * 31L + System.identityHashCode(panel);
            signature = signature * 31L + Float.floatToIntBits(state.getPositionX());
            signature = signature * 31L + Float.floatToIntBits(state.getPositionY());
            signature = signature * 31L + Float.floatToIntBits(panel.getScaledWidth());
            signature = signature * 31L + Float.floatToIntBits(panel.getScaledHeight());
            signature = signature * 31L + Float.floatToIntBits(state.getScale());
            signature = signature * 31L + (canMerge(panel) ? 1L : 0L);
        }
        return signature;
    }

    private boolean canMerge(HudPanel panel) {
        HudHostModule host = panel.hudHostModule();
        return host.drawsPanelChrome();
    }

    /**
     * Merges {@code upper} and {@code lower} when the lower panel's top edge touches the upper panel's bottom edge.
     */
    private void mergeStackedVertically(HudPanel upper, HudPanel lower) {
        ModuleHudState upperState = upper.getHudState();
        ModuleHudState lowerState = lower.getHudState();
        float upperLeft = upperState.getPositionX();
        float upperRight = upperLeft + upper.getScaledWidth();
        float lowerLeft = lowerState.getPositionX();
        float lowerRight = lowerLeft + lower.getScaledWidth();
        float gap = lowerState.getPositionY() - (upperState.getPositionY() + upper.getScaledHeight());
        if (!isTouching(gap)) {
            return;
        }
        if (Math.min(upperRight, lowerRight) - Math.max(upperLeft, lowerLeft) <= EDGE_EPSILON) {
            return;
        }
        if (lowerLeft <= upperLeft + EDGE_EPSILON && lowerRight > upperLeft + EDGE_EPSILON) {
            squareCorners(upperState, RectCornerRoundMask.BOTTOM_LEFT);
        }
        if (lowerRight >= upperRight - EDGE_EPSILON && lowerLeft < upperRight - EDGE_EPSILON) {
            squareCorners(upperState, RectCornerRoundMask.BOTTOM_RIGHT);
        }
        if (upperLeft <= lowerLeft + EDGE_EPSILON && upperRight > lowerLeft + EDGE_EPSILON) {
            squareCorners(lowerState, RectCornerRoundMask.TOP_LEFT);
        }
        if (upperRight >= lowerRight - EDGE_EPSILON && upperLeft < lowerRight - EDGE_EPSILON) {
            squareCorners(lowerState, RectCornerRoundMask.TOP_RIGHT);
        }
    }

    /**
     * Merges {@code left} and {@code right} when the right panel's left edge touches the left panel's right edge.
     */
    private void mergeStackedHorizontally(HudPanel left, HudPanel right) {
        ModuleHudState leftState = left.getHudState();
        ModuleHudState rightState = right.getHudState();
        float leftTop = leftState.getPositionY();
        float leftBottom = leftTop + left.getScaledHeight();
        float rightTop = rightState.getPositionY();
        float rightBottom = rightTop + right.getScaledHeight();
        float gap = rightState.getPositionX() - (leftState.getPositionX() + left.getScaledWidth());
        if (!isTouching(gap)) {
            return;
        }
        if (Math.min(leftBottom, rightBottom) - Math.max(leftTop, rightTop) <= EDGE_EPSILON) {
            return;
        }
        if (rightTop <= leftTop + EDGE_EPSILON && rightBottom > leftTop + EDGE_EPSILON) {
            squareCorners(leftState, RectCornerRoundMask.TOP_RIGHT);
        }
        if (rightBottom >= leftBottom - EDGE_EPSILON && rightTop < leftBottom - EDGE_EPSILON) {
            squareCorners(leftState, RectCornerRoundMask.BOTTOM_RIGHT);
        }
        if (leftTop <= rightTop + EDGE_EPSILON && leftBottom > rightTop + EDGE_EPSILON) {
            squareCorners(rightState, RectCornerRoundMask.TOP_LEFT);
        }
        if (leftBottom >= rightBottom - EDGE_EPSILON && leftTop < rightBottom - EDGE_EPSILON) {
            squareCorners(rightState, RectCornerRoundMask.BOTTOM_LEFT);
        }
    }

    /**
     * Whether two panels touch: the gap between their facing edges is within the edge tolerance.
     */
    private boolean isTouching(float gap) {
        return Math.abs(gap) <= EDGE_EPSILON;
    }

    private void squareCorners(ModuleHudState state, int cornerMask) {
        state.setSquaredCornerMask(state.getSquaredCornerMask() | cornerMask);
    }
}
