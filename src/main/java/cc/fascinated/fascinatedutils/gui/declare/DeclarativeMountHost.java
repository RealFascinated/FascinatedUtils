package cc.fascinated.fascinatedutils.gui.declare;

import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.FReconcileRoot;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

import java.util.function.BiFunction;

/**
 * {@link FWidget} that rebuilds a declarative tree every layout pass from measured dimensions.
 */
public class DeclarativeMountHost extends FWidget {
    private final UiReconciler reconciler = new UiReconciler();
    private final FReconcileRoot reconcileRoot = new FReconcileRoot();
    private final UiReconciler.MountNode rootMount = new UiReconciler.MountNode();
    private final BiFunction<Float, Float, UiView> viewSource;

    public DeclarativeMountHost(BiFunction<Float, Float, UiView> viewSource) {
        this.viewSource = viewSource;
        addChild(reconcileRoot);
    }

    @Override
    public boolean fillsHorizontalInRow() {
        return true;
    }

    @Override
    public boolean fillsVerticalInColumn() {
        return true;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        UiView built = viewSource.apply(layoutWidth, layoutHeight);
        reconciler.sync(reconcileRoot, rootMount, built);
        reconcileRoot.layout(measure, layoutX, layoutY, layoutWidth, layoutHeight);
    }

    /**
     * Fires {@link UiComponent#onUnmount()} for every component in the mounted subtree. Call from
     * owning screens' teardown (alongside {@code FWidgetHost.dispose()}).
     */
    public void dispose() {
        reconciler.disposeAll(rootMount);
        reconcileRoot.clearChildren();
    }
}
