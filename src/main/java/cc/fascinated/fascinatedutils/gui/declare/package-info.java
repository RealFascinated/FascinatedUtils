/**
 * Immutable {@link UiView} descriptions reconciled onto retained {@code FWidget} trees.
 *
 * <p>Authoring: build a tree each layout pass with {@link DeclarativeMountHost} (reconciler output mounts under {@code FReconcileRoot}).
 * Prefer {@link UiComponent} subclasses composed via {@link Ui#component(Class, java.util.function.Supplier, Object)} for any
 * stateful or reusable piece; they give you mount/unmount lifecycle, instance-owned state, and typed props. Use
 * {@link Ui#custom} or {@link Ui#widgetSlot} only for primitives that cannot be expressed declaratively (for example
 * canvas-painting widgets that override {@code renderSelf}).
 *
 * <p>Dynamic lists (waypoints, friends, settings rows) must use stable {@link UiSlot#keyed(String, UiView)} identities so reconcile
 * can reuse widgets. Unkeyed children reconcile by slot index only; avoid that for reorderable collections.
 *
 * <p>Persisted outline fields belong in {@link UiView.UiOutlinedPinned} ({@link Ui#outlinedPinned}) so adapters avoid clobbering
 * user edits on every reconcile.
 *
 * <p>Screens that own a {@link DeclarativeMountHost} should call {@link DeclarativeMountHost#dispose()} from their
 * teardown path so every mounted {@link UiComponent} receives {@code onUnmount()}.
 *
 * <p>The HUD layout editor screen uses canvas-side {@code HudPanel} rendering instead of an {@code FWidgetHost} tree; it is
 * intentionally outside this reconciliation path.
 */
package cc.fascinated.fascinatedutils.gui.declare;
