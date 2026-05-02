package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FColumnWidget extends FWidget {
    private float gap;
    private Align horizontalAlign;

    public FColumnWidget(float gap, Align horizontalAlign) {
        setGapInternal(gap);
        setHorizontalAlignInternal(horizontalAlign);
    }

    private void setGapInternal(float gapValue) {
        this.gap = gapValue;
    }

    private void setHorizontalAlignInternal(Align horizontalAlignValue) {
        this.horizontalAlign = horizontalAlignValue == null ? Align.START : horizontalAlignValue;
    }

    public void setGap(float gapValue) {
        setGapInternal(gapValue);
    }

    public void setHorizontalAlign(Align horizontalAlignValue) {
        setHorizontalAlignInternal(horizontalAlignValue);
    }

    public static float clampScrollOffset(float contentHeight, float viewportHeight, float scrollOffset) {
        float maxScroll = Math.max(0f, contentHeight - viewportHeight);
        return Mth.clamp(scrollOffset, 0f, maxScroll);
    }

    public float gap() {
        return gap;
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        List<FWidget> kids = childrenView();
        if (kids.isEmpty()) {
            return 0f;
        }
        float sum = 0f;
        for (int childIndex = 0; childIndex < kids.size(); childIndex++) {
            FWidget child = kids.get(childIndex);
            sum += child.intrinsicHeightForColumn(measure, widthBudget);
            if (childIndex < kids.size() - 1) {
                sum += gap;
            }
        }
        return sum;
    }

    @Override
    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        float maxWidth = 0f;
        for (FWidget child : childrenView()) {
            float childWidth = child.intrinsicWidthForRow(measure, heightBudget);
            if (childWidth > maxWidth) {
                maxWidth = childWidth;
            }
        }
        return maxWidth;
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
            boolean fillsVertical = child.fillsVerticalInColumn() || constraints.expandVertical();
            if (fillsVertical) {
                fillWeightSum += Math.max(0f, constraints.growWeight());
                mainSizes[childIndex] = Float.NaN;
            }
            else {
                float explicitH = child.intrinsicHeightForColumn(measure, layoutWidth);
                explicitH = Math.max(constraints.minHeight(), Math.min(explicitH, constraints.maxHeight()));
                mainSizes[childIndex] = explicitH;
                fixedSum += explicitH + constraints.marginTop() + constraints.marginBottom();
            }
        }
        fixedSum += gap * Math.max(0, childCount - 1);
        float extra = Math.max(0f, layoutHeight - fixedSum);
        float cursorY = layoutY;
        for (int childIndex = 0; childIndex < childCount; childIndex++) {
            FWidget child = kids.get(childIndex);
            FCellConstraints constraints = child.cellConstraints();
            float marginY = constraints.marginTop() + constraints.marginBottom();
            float allocatedHeight;
            if (Float.isNaN(mainSizes[childIndex]) && fillWeightSum > 0f) {
                float childWeight = Math.max(0f, constraints.growWeight());
                allocatedHeight = extra * (childWeight / fillWeightSum);
            }
            else {
                allocatedHeight = mainSizes[childIndex];
            }
            float childHeight = Math.max(0f, allocatedHeight - marginY);
            float requestedWidth = child.intrinsicWidthForRow(measure, childHeight);
            if (constraints.widthGroupKey() != null && !constraints.widthGroupKey().isEmpty()) {
                requestedWidth = groupedWidths.getOrDefault(constraints.widthGroupKey(), requestedWidth);
            }
            requestedWidth = Math.max(constraints.minWidth(), Math.min(requestedWidth, constraints.maxWidth()));
            float childWidth = child.fillsHorizontalInRow() || constraints.expandHorizontal() ? layoutWidth : (requestedWidth > 0f ? Math.min(requestedWidth, layoutWidth) : layoutWidth);
            childWidth = Math.max(0f, childWidth - constraints.marginStart() - constraints.marginEnd());
            Align resolvedHorizontalAlign = constraints.alignHorizontal() == null ? horizontalAlign : constraints.alignHorizontal();
            float childX = switch (resolvedHorizontalAlign) {
                case CENTER ->
                        layoutX + (layoutWidth - childWidth - constraints.marginStart() - constraints.marginEnd()) * 0.5f + constraints.marginStart();
                case END -> layoutX + layoutWidth - childWidth - constraints.marginEnd();
                case START -> layoutX + constraints.marginStart();
            };
            child.layout(measure, childX, cursorY + constraints.marginTop(), childWidth, childHeight);
            cursorY += Math.max(0f, allocatedHeight);
            if (childIndex < childCount - 1) {
                cursorY += gap;
            }
        }
    }
}
