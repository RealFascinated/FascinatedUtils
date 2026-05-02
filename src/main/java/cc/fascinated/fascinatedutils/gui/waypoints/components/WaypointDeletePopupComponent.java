package cc.fascinated.fascinatedutils.gui.waypoints.components;

import cc.fascinated.fascinatedutils.gui.declare.Ui;
import cc.fascinated.fascinatedutils.gui.declare.UiComponent;
import cc.fascinated.fascinatedutils.gui.declare.UiView;
import cc.fascinated.fascinatedutils.gui.waypoints.WaypointDeletePopupWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

/**
 * Declarative component wrapper around {@link WaypointDeletePopupWidget}. The popup widget itself
 * still owns the retained layout because it is a subclass of {@code FPopupWidget} (custom
 * overlay/escape-handling behaviour); the component simply reuses or re-creates the instance
 * whenever props change.
 */
public class WaypointDeletePopupComponent extends UiComponent<WaypointDeletePopupComponent.Props> {
    private WaypointDeletePopupWidget retainedPopup;
    private String retainedWaypointName;

    public static UiView view(Props props) {
        return Ui.component(WaypointDeletePopupComponent.class, WaypointDeletePopupComponent::new, props);
    }

    @Override
    public UiView render() {
        Props currentProps = props();
        return Ui.custom(previousWidget -> reconcilePopup(previousWidget, currentProps));
    }

    private FWidget reconcilePopup(FWidget previousWidget, Props currentProps) {
        if (retainedPopup != null && previousWidget == retainedPopup && retainedWaypointName.equals(currentProps.waypointName())) {
            return retainedPopup;
        }
        retainedPopup = new WaypointDeletePopupWidget(
                currentProps.waypointName(),
                currentProps.onCancel(),
                currentProps.onConfirm());
        retainedWaypointName = currentProps.waypointName();
        return retainedPopup;
    }

    /**
     * Props for {@link WaypointDeletePopupComponent}.
     *
     * @param waypointName name to embed in the confirmation message
     * @param onCancel     invoked when the user cancels or clicks outside
     * @param onConfirm    invoked when the user confirms deletion
     */
    public record Props(String waypointName, Runnable onCancel, Runnable onConfirm) {
    }
}
