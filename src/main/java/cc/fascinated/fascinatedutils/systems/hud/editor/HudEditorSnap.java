package cc.fascinated.fascinatedutils.systems.hud.editor;

import cc.fascinated.fascinatedutils.systems.hud.panel.HudPanel;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class HudEditorSnap {

    private static final float SNAP_RADIUS = 8f;

    /**
     * Snap targets inset from the logical screen edge so widgets can align to a safe margin (matches guide lines).
     */
    private static final float SCREEN_EDGE_SNAP_INSET = 5f;

    private static final SnapAxis HORIZONTAL_AXIS = new SnapAxis(widget -> widget.getHudState().getPositionX(), HudPanel::getScaledWidth);
    private static final SnapAxis VERTICAL_AXIS = new SnapAxis(widget -> widget.getHudState().getPositionY(), HudPanel::getScaledHeight);

    /**
     * Snap a widget top-left to the nearest eligible X and Y within the snap radius, then clamp to the canvas.
     */
    public static SnapResult snapTopLeft(float rawLeft, float rawTop, float widgetWidth, float widgetHeight, float canvasWidth, float canvasHeight, List<HudPanel> allWidgets, HudPanel exclude) {
        float maxLeft = Math.max(0f, canvasWidth - widgetWidth);
        float maxTop = Math.max(0f, canvasHeight - widgetHeight);
        List<SnapCandidate> horizontalCandidates = collectSnapTargets(allWidgets, exclude, VERTICAL_AXIS, rawTop, widgetHeight, HORIZONTAL_AXIS, widgetWidth);
        addEdgeSnapTargets(horizontalCandidates, maxLeft, canvasWidth, widgetWidth);
        List<SnapCandidate> verticalCandidates = collectSnapTargets(allWidgets, exclude, HORIZONTAL_AXIS, rawLeft, widgetWidth, VERTICAL_AXIS, widgetHeight);
        addEdgeSnapTargets(verticalCandidates, maxTop, canvasHeight, widgetHeight);
        AxisSnapResult snappedLeft = snapOneAxis(rawLeft, maxLeft, horizontalCandidates);
        AxisSnapResult snappedTop = snapOneAxis(rawTop, maxTop, verticalCandidates);
        return new SnapResult(snappedLeft.snappedCoord(), snappedTop.snappedCoord(), snappedLeft.guideCoord(), snappedTop.guideCoord(), snappedLeft.isCenter(), snappedTop.isCenter());
    }

    /**
     * Collects snap coordinates along {@code snapAxis} from widgets whose extent on {@code overlapAxis} is within the snap
     * radius of the dragged widget's extent on that same axis.
     */
    private static List<SnapCandidate> collectSnapTargets(List<HudPanel> allWidgets, HudPanel exclude, SnapAxis overlapAxis, float dragOverlapStart, float dragOverlapSize, SnapAxis snapAxis, float dragSnapSize) {
        List<SnapCandidate> targets = new ArrayList<>();
        float dragOverlapEnd = dragOverlapStart + dragOverlapSize;
        for (HudPanel other : allWidgets) {
            if (other == exclude) {
                continue;
            }
            float otherOverlapStart = overlapAxis.position().of(other);
            float otherOverlapEnd = otherOverlapStart + overlapAxis.size().of(other);
            if (intervalGap(dragOverlapStart, dragOverlapEnd, otherOverlapStart, otherOverlapEnd) > SNAP_RADIUS) {
                continue;
            }
            float otherStart = snapAxis.position().of(other);
            float otherSize = snapAxis.size().of(other);
            targets.add(new SnapCandidate(otherStart, otherStart));
            targets.add(new SnapCandidate(otherStart + otherSize, otherStart + otherSize));
            targets.add(new SnapCandidate(otherStart - dragSnapSize, otherStart));
            targets.add(new SnapCandidate(otherStart + otherSize - dragSnapSize, otherStart + otherSize));
        }
        return targets;
    }

    private static void addEdgeSnapTargets(List<SnapCandidate> targets, float maxCoord, float canvasSize, float widgetSize) {
        targets.add(new SnapCandidate(0f, 0f));
        targets.add(new SnapCandidate(maxCoord, canvasSize));
        targets.add(new SnapCandidate(SCREEN_EDGE_SNAP_INSET, SCREEN_EDGE_SNAP_INSET));
        targets.add(new SnapCandidate(canvasSize - widgetSize - SCREEN_EDGE_SNAP_INSET, canvasSize - SCREEN_EDGE_SNAP_INSET));
        targets.add(new SnapCandidate((canvasSize - widgetSize) * 0.5f, (float) Math.floor(canvasSize * 0.5f), true));
    }

    private static float intervalGap(float firstStart, float firstEnd, float secondStart, float secondEnd) {
        if (firstEnd >= secondStart && secondEnd >= firstStart) {
            return 0f;
        }
        if (firstEnd < secondStart) {
            return secondStart - firstEnd;
        }
        return firstStart - secondEnd;
    }

    private static AxisSnapResult snapOneAxis(float raw, float maxCoord, List<SnapCandidate> candidates) {
        float best = Mth.clamp(raw, 0f, maxCoord);
        float bestDistance = HudEditorSnap.SNAP_RADIUS;
        float bestGuide = Float.NaN;
        boolean bestIsCenter = false;
        for (SnapCandidate candidate : candidates) {
            float clamped = Mth.clamp(candidate.snapCoord(), 0f, maxCoord);
            float distance = Math.abs(raw - clamped);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = clamped;
                bestGuide = candidate.guideCoord();
                bestIsCenter = candidate.isCenter();
            }
        }
        return new AxisSnapResult(Mth.clamp(best, 0f, maxCoord), bestGuide, bestIsCenter);
    }

    public record SnapResult(float snappedLeft, float snappedTop, float verticalGuideX, float horizontalGuideY,
                             boolean verticalGuideIsCenter, boolean horizontalGuideIsCenter) {
        public boolean hasVerticalGuide() {
            return Float.isFinite(verticalGuideX);
        }

        public boolean hasHorizontalGuide() {
            return Float.isFinite(horizontalGuideY);
        }
    }

    private interface AxisValue {
        float of(HudPanel widget);
    }

    private record SnapAxis(AxisValue position, AxisValue size) {}

    private record SnapCandidate(float snapCoord, float guideCoord, boolean isCenter) {
        SnapCandidate(float snapCoord, float guideCoord) {
            this(snapCoord, guideCoord, false);
        }
    }

    private record AxisSnapResult(float snappedCoord, float guideCoord, boolean isCenter) {}
}
