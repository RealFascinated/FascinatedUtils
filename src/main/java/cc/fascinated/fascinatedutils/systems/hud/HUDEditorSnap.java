package cc.fascinated.fascinatedutils.systems.hud;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class HUDEditorSnap {

    private static final float SNAP_RADIUS = 8f;
    private static final float SIDE_CENTER_ZONE = 24f;

    /**
     * Snap targets inset from the logical screen edge so widgets can align to a safe margin (matches guide lines).
     */
    private static final float SCREEN_EDGE_SNAP_INSET = 5f;

    /**
     * Snap a widget top-left to the nearest eligible X and Y within the snap radius, then clamp to the canvas.
     */
    public static SnapResult snapTopLeft(float rawLeft, float rawTop, float widgetWidth, float widgetHeight, float canvasWidth, float canvasHeight, List<HudModule> allWidgets, HudModule exclude) {
        float maxLeft = Math.max(0f, canvasWidth - widgetWidth);
        float maxTop = Math.max(0f, canvasHeight - widgetHeight);
        List<SnapCandidate> horizontalCandidates = collectHorizontalSnapTargets(allWidgets, exclude, rawTop, widgetWidth, widgetHeight);
        List<SnapCandidate> verticalCandidates = collectVerticalSnapTargets(allWidgets, exclude, rawLeft, widgetWidth, widgetHeight);
        horizontalCandidates.add(new SnapCandidate(0f, 0f));
        horizontalCandidates.add(new SnapCandidate(maxLeft, canvasWidth));
        horizontalCandidates.add(new SnapCandidate(SCREEN_EDGE_SNAP_INSET, SCREEN_EDGE_SNAP_INSET));
        horizontalCandidates.add(new SnapCandidate(canvasWidth - widgetWidth - SCREEN_EDGE_SNAP_INSET, canvasWidth - SCREEN_EDGE_SNAP_INSET));
        boolean nearTopSide = rawTop <= SIDE_CENTER_ZONE;
        boolean nearBottomSide = rawTop + widgetHeight >= canvasHeight - SIDE_CENTER_ZONE;
        if (nearTopSide || nearBottomSide) {
            horizontalCandidates.add(new SnapCandidate((canvasWidth - widgetWidth) * 0.5f, canvasWidth * 0.5f));
        }
        verticalCandidates.add(new SnapCandidate(0f, 0f));
        verticalCandidates.add(new SnapCandidate(maxTop, canvasHeight));
        verticalCandidates.add(new SnapCandidate(SCREEN_EDGE_SNAP_INSET, SCREEN_EDGE_SNAP_INSET));
        verticalCandidates.add(new SnapCandidate(canvasHeight - widgetHeight - SCREEN_EDGE_SNAP_INSET, canvasHeight - SCREEN_EDGE_SNAP_INSET));
        boolean nearLeftSide = rawLeft <= SIDE_CENTER_ZONE;
        boolean nearRightSide = rawLeft + widgetWidth >= canvasWidth - SIDE_CENTER_ZONE;
        if (nearLeftSide || nearRightSide) {
            verticalCandidates.add(new SnapCandidate((canvasHeight - widgetHeight) * 0.5f, canvasHeight * 0.5f));
        }
        AxisSnapResult snappedLeft = snapOneAxis(rawLeft, maxLeft, horizontalCandidates);
        AxisSnapResult snappedTop = snapOneAxis(rawTop, maxTop, verticalCandidates);
        return new SnapResult(snappedLeft.snappedCoord(), snappedTop.snappedCoord(), snappedLeft.guideCoord(), snappedTop.guideCoord());
    }

    private static List<SnapCandidate> collectHorizontalSnapTargets(List<HudModule> allWidgets, HudModule exclude, float rawTop, float dragWidth, float dragHeight) {
        List<SnapCandidate> targets = new ArrayList<>();
        float draggingBottom = rawTop + dragHeight;
        for (HudModule other : allWidgets) {
            if (other == exclude) {
                continue;
            }
            float otherTop = other.getHudState().getPositionY();
            float otherBottom = otherTop + other.getScaledHeight();
            if (intervalGap(rawTop, draggingBottom, otherTop, otherBottom) > SNAP_RADIUS) {
                continue;
            }
            float otherX = other.getHudState().getPositionX();
            float otherWidth = other.getScaledWidth();
            targets.add(new SnapCandidate(otherX, otherX));
            targets.add(new SnapCandidate(otherX + otherWidth, otherX + otherWidth));
            targets.add(new SnapCandidate(otherX - dragWidth, otherX));
            targets.add(new SnapCandidate(otherX + otherWidth - dragWidth, otherX + otherWidth));
        }
        return targets;
    }

    private static List<SnapCandidate> collectVerticalSnapTargets(List<HudModule> allWidgets, HudModule exclude, float rawLeft, float dragWidth, float dragHeight) {
        List<SnapCandidate> targets = new ArrayList<>();
        float draggingRight = rawLeft + dragWidth;
        for (HudModule other : allWidgets) {
            if (other == exclude) {
                continue;
            }
            float otherLeft = other.getHudState().getPositionX();
            float otherRight = otherLeft + other.getScaledWidth();
            if (intervalGap(rawLeft, draggingRight, otherLeft, otherRight) > SNAP_RADIUS) {
                continue;
            }
            float otherY = other.getHudState().getPositionY();
            float otherHeight = other.getScaledHeight();
            targets.add(new SnapCandidate(otherY, otherY));
            targets.add(new SnapCandidate(otherY + otherHeight, otherY + otherHeight));
            targets.add(new SnapCandidate(otherY - dragHeight, otherY));
            targets.add(new SnapCandidate(otherY + otherHeight - dragHeight, otherY + otherHeight));
        }
        return targets;
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
        float bestDistance = HUDEditorSnap.SNAP_RADIUS;
        float bestGuide = Float.NaN;
        for (SnapCandidate candidate : candidates) {
            float clamped = Mth.clamp(candidate.snapCoord(), 0f, maxCoord);
            float distance = Math.abs(raw - clamped);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = clamped;
                bestGuide = candidate.guideCoord();
            }
        }
        return new AxisSnapResult(Mth.clamp(best, 0f, maxCoord), bestGuide);
    }

    public record SnapResult(float snappedLeft, float snappedTop, float verticalGuideX, float horizontalGuideY) {
        public boolean hasVerticalGuide() {
            return Float.isFinite(verticalGuideX);
        }

        public boolean hasHorizontalGuide() {
            return Float.isFinite(horizontalGuideY);
        }
    }

    private record SnapCandidate(float snapCoord, float guideCoord) {}

    private record AxisSnapResult(float snappedCoord, float guideCoord) {}
}
