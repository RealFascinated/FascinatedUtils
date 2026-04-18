package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FRowWidget extends FWidget {
    private final float gap;
    private final Align verticalAlign;

    public FRowWidget(float gap, Align verticalAlign) {
        this.gap = gap;
        this.verticalAlign = verticalAlign == null ? Align.START : verticalAlign;
    }

    @Override
    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        List<FWidget> kids = childrenView();
        if (kids.isEmpty()) {
            return 0f;
        }
        float sum = 0f;
        Map<String, Float> groupedWidths = new HashMap<>();
        for (int childIndex = 0; childIndex < kids.size(); childIndex++) {
            FWidget child = kids.get(childIndex);
            FCellConstraints constraints = child.cellConstraints();
            float intrinsicWidth = child.intrinsicWidthForRow(measure, heightBudget);
            if (constraints.widthGroupKey() != null && !constraints.widthGroupKey().isEmpty()) {
                groupedWidths.merge(constraints.widthGroupKey(), intrinsicWidth, Math::max);
            }
        }
        for (int childIndex = 0; childIndex < kids.size(); childIndex++) {
            FWidget child = kids.get(childIndex);
            FCellConstraints constraints = child.cellConstraints();
            if (!child.fillsHorizontalInRow()) {
                float intrinsicWidth = child.intrinsicWidthForRow(measure, heightBudget);
                if (constraints.widthGroupKey() != null && !constraints.widthGroupKey().isEmpty()) {
                    intrinsicWidth = groupedWidths.getOrDefault(constraints.widthGroupKey(), intrinsicWidth);
                }
                intrinsicWidth = Math.max(constraints.minWidth(), Math.min(intrinsicWidth, constraints.maxWidth()));
                sum += intrinsicWidth + constraints.marginStart() + constraints.marginEnd();
            }
            if (childIndex < kids.size() - 1) {
                sum += gap;
            }
        }
        return sum;
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        float maxHeight = 0f;
        for (FWidget child : childrenView()) {
            float childHeight = child.intrinsicHeightForColumn(measure, widthBudget);
            if (childHeight > maxHeight) {
                maxHeight = childHeight;
            }
        }
        return maxHeight;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        List<FWidget> kids = childrenView();
        if (kids.isEmpty()) {
            return;
        }
        int childCount = kids.size();
        float[] mainSizes = new float[childCount];
        float fixedSum = 0f;
        float fillWeightSum = 0f;
        Map<String, Float> groupedWidths = new HashMap<>();
        for (FWidget child : kids) {
            FCellConstraints constraints = child.cellConstraints();
            if (constraints.widthGroupKey() == null || constraints.widthGroupKey().isEmpty()) {
                continue;
            }
            float intrinsicWidth = child.intrinsicWidthForRow(measure, layoutHeight);
            groupedWidths.merge(constraints.widthGroupKey(), intrinsicWidth, Math::max);
        }
        for (int childIndex = 0; childIndex < childCount; childIndex++) {
            FWidget child = kids.get(childIndex);
            FCellConstraints constraints = child.cellConstraints();
            boolean fillsHorizontal = child.fillsHorizontalInRow() || constraints.expandHorizontal();
            if (fillsHorizontal) {
                fillWeightSum += Math.max(0f, constraints.growWeight());
                mainSizes[childIndex] = Float.NaN;
            }
            else {
                float intrinsicW = child.intrinsicWidthForRow(measure, layoutHeight);
                if (constraints.widthGroupKey() != null && !constraints.widthGroupKey().isEmpty()) {
                    intrinsicW = groupedWidths.getOrDefault(constraints.widthGroupKey(), intrinsicW);
                }
                intrinsicW = Math.max(constraints.minWidth(), Math.min(intrinsicW, constraints.maxWidth()));
                mainSizes[childIndex] = intrinsicW;
                fixedSum += intrinsicW + constraints.marginStart() + constraints.marginEnd();
            }
        }
        fixedSum += gap * Math.max(0, childCount - 1);
        float extra = Math.max(0f, layoutWidth - fixedSum);
        float cursorX = layoutX;
        for (int childIndex = 0; childIndex < childCount; childIndex++) {
            FWidget child = kids.get(childIndex);
            FCellConstraints constraints = child.cellConstraints();
            float marginX = constraints.marginStart() + constraints.marginEnd();
            float allocatedWidth;
            if (Float.isNaN(mainSizes[childIndex]) && fillWeightSum > 0f) {
                float childWeight = Math.max(0f, constraints.growWeight());
                allocatedWidth = extra * (childWeight / fillWeightSum);
            }
            else {
                allocatedWidth = mainSizes[childIndex];
            }
            float childWidth = Math.max(0f, allocatedWidth - marginX);
            float requestedHeight = child.intrinsicHeightForColumn(measure, childWidth);
            requestedHeight = Math.max(constraints.minHeight(), Math.min(requestedHeight, constraints.maxHeight()));
            float childHeight = child.fillsVerticalInColumn() || constraints.expandVertical() ? layoutHeight : (requestedHeight > 0f ? Math.min(requestedHeight, layoutHeight) : layoutHeight);
            childHeight = Math.max(0f, childHeight - constraints.marginTop() - constraints.marginBottom());
            Align resolvedVerticalAlign = constraints.alignVertical() == null ? verticalAlign : constraints.alignVertical();
            float childY = switch (resolvedVerticalAlign) {
                case CENTER ->
                        layoutY + (layoutHeight - childHeight - constraints.marginTop() - constraints.marginBottom()) * 0.5f + constraints.marginTop();
                case END -> layoutY + layoutHeight - childHeight - constraints.marginBottom();
                case START -> layoutY + constraints.marginTop();
            };
            child.layout(measure, cursorX + constraints.marginStart(), childY, childWidth, childHeight);
            cursorX += Math.max(0f, allocatedWidth);
            if (childIndex < childCount - 1) {
                cursorX += gap;
            }
        }
    }
}
