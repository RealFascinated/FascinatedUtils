package cc.fascinated.fascinatedutils.gui.declare;

import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import org.jspecify.annotations.Nullable;

/**
 * User hook for mounting custom {@link FWidget} subtrees while still participating in reconciliation.
 */
@FunctionalInterface
public interface UiBacking {
    /**
     * Returns the widget to attach; reuse {@code previousWidget} when compatible to preserve state.
     *
     * @param previousWidget widget returned on the last pass, or {@code null} on first mount
     * @return widget to display
     */
    FWidget reconcilePrevious(@Nullable FWidget previousWidget);

    /**
     * Convenience wrapper that fixes the returned widget type for keyed slots.
     *
     * @param <W> widget type
     */
    @FunctionalInterface
    interface Typed<W extends FWidget> extends UiBacking {
        W reconcileTyped(@Nullable FWidget previousWidget);

        @Override
        default FWidget reconcilePrevious(@Nullable FWidget previousWidget) {
            return reconcileTyped(previousWidget);
        }
    }
}
