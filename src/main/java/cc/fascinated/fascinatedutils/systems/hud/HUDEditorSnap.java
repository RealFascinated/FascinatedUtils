package cc.fascinated.fascinatedutils.systems.hud;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class HUDEditorSnap {

    private static final float SNAP_RADIUS = 8f;

    /**
     * Snap targets inset from the logical screen edge so widgets can align to a safe margin (matches guide lines).
     */
    private static final float SCREEN_EDGE_SNAP_INSET = 5f;

    /**
     * Snap a widget top-left to the nearest eligible X and Y within the snap radius, then clamp to the canvas.
     */
    public static SnapResult snapTopLeft(float rawLeft, float rawTop, float widgetWidth, float widgetHeight, float canvasWidth, float canvasHeight, List<HudPanel> allWidgets, HudPanel exclude) {
        float maxLeft = Math.max(0f, canvasWidth - widgetWidth);
        float maxTop = Math.max(0f, canvasHeight - widgetHeight);
        List<SnapCandidate> horizontalCandidates = collectHorizontalSnapTargets(allWidgets, exclude, rawTop, widgetWidth, widgetHeight);
        List<SnapCandidate> verticalCandidates = collectVerticalSnapTargets(allWidgets, exclude, rawLeft, widgetWidth, widgetHeight);
        horizontalCandidates.add(new SnapCandidate(0f, 0f));
        horizontalCandidates.add(new SnapCandidate(maxLeft, canvasWidth));
        horizontalCandidates.add(new SnapCandidate(SCREEN_EDGE_SNAP_INSET, SCREEN_EDGE_SNAP_INSET));
        horizontalCandidates.add(new SnapCandidate(canvasWidth - widgetWidth - SCREEN_EDGE_SNAP_INSET, canvasWidth - SCREEN_EDGE_SNAP_INSET));
        horizontalCandidates.add(new SnapCandidate((canvasWidth - widgetWidth) * 0.5f, (float) Math.floor(canvasWidth * 0.5f), true));
        verticalCandidates.add(new SnapCandidate(0f, 0f));
        verticalCandidates.add(new SnapCandidate(maxTop, canvasHeight));
        verticalCandidates.add(new SnapCandidate(SCREEN_EDGE_SNAP_INSET, SCREEN_EDGE_SNAP_INSET));
        verticalCandidates.add(new SnapCandidate(canvasHeight - widgetHeight - SCREEN_EDGE_SNAP_INSET, canvasHeight - SCREEN_EDGE_SNAP_INSET));
        verticalCandidates.add(new SnapCandidate((canvasHeight - widgetHeight) * 0.5f, (float) Math.floor(canvasHeight * 0.5f), true));
        AxisSnapResult snappedLeft = snapOneAxis(rawLeft, maxLeft, horizontalCandidates);
        AxisSnapResult snappedTop = snapOneAxis(rawTop, maxTop, verticalCandidates);
        return new SnapResult(snappedLeft.snappedCoord(), snappedTop.snappedCoord(), snappedLeft.guideCoord(), snappedTop.guideCoord(), snappedLeft.isCenter(), snappedTop.isCenter());
    }

    private static List<SnapCandidate> collectHorizontalSnapTargets(List<HudPanel> allWidgets, HudPanel exclude, float rawTop, float dragWidth, float dragHeight) {
        List<SnapCandidate> targets = new ArrayList<>();
        float draggingBottom = rawTop + dragHeight;
        for (HudPanel other : allWidgets) {
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

    private static List<SnapCandidate> collectVerticalSnapTargets(List<HudPanel> allWidgets, HudPanel exclude, float rawLeft, float dragWidth, float dragHeight) {
        List<SnapCandidate> targets = new ArrayList<>();
        float draggingRight = rawLeft + dragWidth;
        for (HudPanel other : allWidgets) {
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

    public record SnapResult(float snappedLeft, float snappedTop, float verticalGuideX, float horizontalGuideY, boolean verticalGuideIsCenter, boolean horizontalGuideIsCenter) {
        public boolean hasVerticalGuide() {
            return Float.isFinite(verticalGuideX);
        }

        public boolean hasHorizontalGuide() {
            return Float.isFinite(horizontalGuideY);
        }
    }

    private record SnapCandidate(float snapCoord, float guideCoord, boolean isCenter) {
        SnapCandidate(float snapCoord, float guideCoord) {
            this(snapCoord, guideCoord, false);
        }
    }

    private record AxisSnapResult(float snappedCoord, float guideCoord, boolean isCenter) {}
}
