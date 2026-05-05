package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.FConfirmPopupWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

/**
 * Confirmation overlay that covers the full viewport and centers the confirm dialog.
 */
public final class SocialDestructiveFullscreenConfirmOverlay {

    /**
     * Creates a fullscreen confirm overlay widget with the given props.
     *
     * @param props overlay configuration
     * @return widget ready for inclusion in the layout tree
     */
    public static FWidget create(Props props) {
        return new PopupHost(props);
    }

    /**
     * Input shared by fullscreen-sized overlay plus confirm/deny copy and callbacks.
     *
     * @param title        dialog title
     * @param message      optional body message
     * @param confirmLabel confirm button text
     * @param denyLabel    deny button text
     * @param onDeny       cancel / dismiss hook
     * @param onConfirm    confirm hook
     */
    public record Props(String title, String message, String confirmLabel, String denyLabel, Runnable onDeny,
                        Runnable onConfirm) {}

    private static final class PopupHost extends FWidget {

        PopupHost(Props props) {
            addChild(new FConfirmPopupWidget(
                    props.title(), props.message(), props.confirmLabel(), props.denyLabel(),
                    FConfirmPopupWidget.ConfirmStyle.DESTRUCTIVE, props.onDeny(), props.onConfirm()));
        }

        @Override
        public PointerHitKind pointerHitKind() {
            return PointerHitKind.BLOCK;
        }

        @Override
        public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
            setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
            FConfirmPopupWidget popup = (FConfirmPopupWidget) childrenView().get(0);
            popup.layout(measure, layoutX, layoutY, layoutWidth, layoutHeight);
        }

        @Override
        public boolean mouseDown(float pointerX, float pointerY, int button) {
            FConfirmPopupWidget popup = (FConfirmPopupWidget) childrenView().get(0);
            return popup.mouseDown(pointerX, pointerY, button);
        }
    }
}
