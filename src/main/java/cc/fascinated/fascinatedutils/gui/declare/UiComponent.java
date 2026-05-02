package cc.fascinated.fascinatedutils.gui.declare;

/**
 * Base class for a retained, stateful component rendered through the declarative reconciler.
 *
 * <p>Concrete subclasses declare their own props record, hold instance state as fields, and
 * implement {@link #render()} to return an immutable {@link UiView} tree each reconcile pass.
 * The reconciler preserves the same instance across frames as long as the component class
 * matches (see {@link UiView.UiComponentNode}); props are rebound before each render.
 *
 * @param <P> props type (typically a record)
 */
public abstract class UiComponent<P> {
    private P currentProps;
    private boolean mounted;

    /**
     * Current props bound by the reconciler; {@code null} before the first render.
     *
     * @return latest props
     */
    protected final P props() {
        return currentProps;
    }

    /**
     * Whether this component is currently mounted; {@code false} once {@link #onUnmount()} fires.
     *
     * @return mount flag
     */
    protected final boolean isMounted() {
        return mounted;
    }

    /**
     * Internal — called by {@link UiReconciler} before each render pass with the latest props.
     *
     * @param nextProps props for the upcoming render
     */
    final void bindProps(P nextProps) {
        this.currentProps = nextProps;
    }

    /**
     * Internal — called by {@link UiReconciler} the first time this instance is mounted.
     */
    final void dispatchMount() {
        if (mounted) {
            return;
        }
        mounted = true;
        onMount();
    }

    /**
     * Internal — called by {@link UiReconciler} when the component is being unmounted.
     */
    final void dispatchUnmount() {
        if (!mounted) {
            return;
        }
        mounted = false;
        onUnmount();
    }

    /**
     * Lifecycle hook fired once when the component first mounts into the tree.
     *
     * <p>Override for one-time setup such as registering listeners. Props are available via
     * {@link #props()}.
     */
    protected void onMount() {
    }

    /**
     * Lifecycle hook fired when the component leaves the tree (class mismatch, slot key removed,
     * or host dispose). Override for cleanup such as unregistering listeners.
     */
    protected void onUnmount() {
    }

    /**
     * Returns the immutable view tree for the current {@link #props()} and instance state.
     * Called by the reconciler on every reconcile pass after {@link #bindProps(Object)}.
     *
     * @return declarative view tree
     */
    public abstract UiView render();
}
