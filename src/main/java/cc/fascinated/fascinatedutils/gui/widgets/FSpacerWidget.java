package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FSpacerWidget extends FWidget {
    private final float spacerWidth;
    private final float spacerHeight;

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        float useW = spacerWidth > 0f ? spacerWidth : layoutWidth;
        float useH = spacerHeight > 0f ? spacerHeight : layoutHeight;
        setBounds(layoutX, layoutY, useW, useH);
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        return spacerHeight > 0f ? spacerHeight : 0f;
    }

    @Override
    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        return spacerWidth > 0f ? spacerWidth : 0f;
    }
}
