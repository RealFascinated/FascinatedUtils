package cc.fascinated.fascinatedutils.gui.widgets.settings;

import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

/**
 * Abstract base for all setting row widgets.
 *
 * <p>Holds the common fields and provides the boilerplate overrides that are identical across every
 * setting type: fixed intrinsic dimensions, the pass-through {@code layout}, pointer-interest, and
 * the shared {@code rectContains} hit-test helper.
 */
public abstract class FSettingRowWidget extends FWidget {

    protected final float outerWidth;
    protected final float outerHeight;
    protected final Runnable onPersist;
    protected final float valueColumnStartX;

    protected FSettingRowWidget(float outerWidth, float outerHeight, Runnable onPersist, float valueColumnStartX) {
        this.outerWidth = outerWidth;
        this.outerHeight = outerHeight;
        this.onPersist = onPersist;
        this.valueColumnStartX = Math.max(0f, valueColumnStartX);
    }

    protected FSettingRowWidget(float outerWidth, float outerHeight, Runnable onPersist) {
        this(outerWidth, outerHeight, onPersist, 0f);
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        return outerHeight;
    }

    @Override
    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        return outerWidth;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
    }

    @Override
    public boolean wantsPointer() {
        return true;
    }

    /**
     * Hit-tests a {@code float[4]} bounding rect of the form {@code [left, top, width, height]}.
     *
     * @param rect     bounding rect as {@code [left, top, width, height]}
     * @param pointerX pointer X in logical pixels
     * @param pointerY pointer Y in logical pixels
     * @return true if the pointer is inside the rect
     */
    protected static boolean rectContains(float[] rect, float pointerX, float pointerY) {
        return pointerX >= rect[0] && pointerY >= rect[1] && pointerX < rect[0] + rect[2] && pointerY < rect[1] + rect[3];
    }
}
