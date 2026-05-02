package cc.fascinated.fascinatedutils.gui.waypoints.components;

import cc.fascinated.fascinatedutils.gui.declare.Ui;
import cc.fascinated.fascinatedutils.gui.declare.UiComponent;
import cc.fascinated.fascinatedutils.gui.declare.UiView;
import cc.fascinated.fascinatedutils.gui.waypoints.WaypointRenamePopupWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

/**
 * Component wrapper around {@link WaypointRenamePopupWidget}. The retained popup keeps its text
 * input mounted for focus/IME stability; the component re-creates it when the inbound name
 * changes to reset the field.
 */
public class WaypointRenamePopupComponent extends UiComponent<WaypointRenamePopupComponent.Props> {
    private WaypointRenamePopupWidget retainedPopup;
    private String retainedName;

    public static UiView view(Props props) {
        return Ui.component(WaypointRenamePopupComponent.class, WaypointRenamePopupComponent::new, props);
    }

    @Override
    public UiView render() {
        Props currentProps = props();
        return Ui.custom(previousWidget -> reconcilePopup(previousWidget, currentProps));
    }

    private FWidget reconcilePopup(FWidget previousWidget, Props currentProps) {
        if (retainedPopup != null && previousWidget == retainedPopup && retainedName.equals(currentProps.currentName())) {
            return retainedPopup;
        }
        retainedPopup = new WaypointRenamePopupWidget(
                currentProps.currentName(),
                currentProps.onCancel(),
                newName -> currentProps.onSubmit().accept(newName));
        retainedName = currentProps.currentName();
        return retainedPopup;
    }

    /**
     * Props for {@link WaypointRenamePopupComponent}.
     *
     * @param currentName initial text in the rename field; identity change re-seeds the popup
     * @param onCancel    invoked when the user cancels or clicks outside
     * @param onSubmit    invoked with the trimmed new name
     */
    public record Props(String currentName, Runnable onCancel, java.util.function.Consumer<String> onSubmit) {
    }
}
