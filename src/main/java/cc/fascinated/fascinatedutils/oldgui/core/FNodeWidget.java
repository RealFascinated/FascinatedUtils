package cc.fascinated.fascinatedutils.oldgui.core;

import cc.fascinated.fascinatedutils.oldgui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.oldgui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.oldgui.widgets.FWidget;

/**
 * An {@link FWidget} that delegates all layout and rendering to the widget produced by a
 * {@link FWidgetNode}. State owned by the node is preserved across re-renders as long as
 * this host widget is kept in the tree.
 */
public class FNodeWidget extends FWidget {
    private final FWidgetNode node;
    private FWidget resolved;

    public FNodeWidget(FWidgetNode node) {
        this.node = node;
    }

    private FWidget resolved() {
        FWidget next = node.resolveWidget();
        if (next != resolved) {
            clearChildren();
            addChild(next);
            resolved = next;
        }
        return resolved;
    }

    @Override
    public boolean fillsHorizontalInRow() {
        return resolved().fillsHorizontalInRow();
    }

    @Override
    public boolean fillsVerticalInColumn() {
        return resolved().fillsVerticalInColumn();
    }

    @Override
    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        return resolved().intrinsicWidthForRow(measure, heightBudget);
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        return resolved().intrinsicHeightForColumn(measure, widthBudget);
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        resolved().layout(measure, layoutX, layoutY, layoutWidth, layoutHeight);
    }

    @Override
    public void render(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
        resolved().render(graphics, frame, deltaSeconds);
    }
}
