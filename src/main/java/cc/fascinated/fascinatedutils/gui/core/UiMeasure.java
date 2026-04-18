package cc.fascinated.fascinatedutils.gui.core;

import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class UiMeasure {

    public static float estimateColumnContentHeight(List<FWidget> rows, float gapBetweenRows, float columnInnerWidth, UIRenderer renderer) {
        if (rows == null || rows.isEmpty()) {
            return 0f;
        }
        float sumHeight = 0f;
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            sumHeight += rows.get(rowIndex).intrinsicHeightForColumn(renderer, columnInnerWidth);
            if (rowIndex < rows.size() - 1) {
                sumHeight += gapBetweenRows;
            }
        }
        return sumHeight;
    }

    public static float[] hudTwoRowPanelSize(String titleLine, String hintLine, float pad, float gapBetweenRows, float rowHeight, UIRenderer renderer) {
        int titleWidth = renderer.measureTextWidth(titleLine, false);
        int hintWidth = renderer.measureTextWidth(hintLine, false);
        float panelWidth = Math.max(titleWidth, hintWidth) + 2f * pad;
        float panelHeight = pad + rowHeight + gapBetweenRows + rowHeight + pad;
        return new float[]{panelWidth, panelHeight};
    }
}
