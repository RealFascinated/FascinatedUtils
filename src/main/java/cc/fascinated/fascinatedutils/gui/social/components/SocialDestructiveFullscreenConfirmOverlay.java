package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.declare.Ui;
import cc.fascinated.fascinatedutils.gui.declare.UiComponent;
import cc.fascinated.fascinatedutils.gui.declare.UiView;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.FConfirmPopupWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

/**
 * Confirmation overlay anchored to the right-side social dock panel, matching legacy layout where
 * the popup is constrained to panel width rather than the full viewport.
 */
public final class SocialDestructiveFullscreenConfirmOverlay extends UiComponent<SocialDestructiveFullscreenConfirmOverlay.Props> {
    private static final float PANEL_WIDTH = 250f;

    /**
     * Mounts this overlay subtree (props are rebound every reconcile pass).
     *
     * @param props declarative overlay configuration
     * @return view node consumed by {@link cc.fascinated.fascinatedutils.gui.declare.DeclarativeMountHost}
     */
    public static UiView view(Props props) {
        return Ui.component(SocialDestructiveFullscreenConfirmOverlay.class, SocialDestructiveFullscreenConfirmOverlay::new, props);
    }

    @Override
    public UiView render() {
        Props overlayProps = props();
        return Ui.custom(previousWidget -> previousWidget instanceof PopupHost existing ? existing.refresh(overlayProps) : new PopupHost(overlayProps));
    }

    /**
     * Input shared by fullscreen-sized overlay plus confirm/deny copy and callbacks.
     *
     * @param title           dialog title
     * @param message         optional body message
     * @param confirmLabel    confirm button text
     * @param denyLabel       deny button text
     * @param onDeny          cancel / dismiss hook
     * @param onConfirm       confirm hook
     */
    public record Props(String title,
                        String message,
                        String confirmLabel,
                        String denyLabel,
                        Runnable onDeny,
                        Runnable onConfirm) {
        private Props textualCopy() {
            return new Props(title, message, confirmLabel, denyLabel, () -> {}, () -> {});
        }
    }

    private static final class PopupHost extends FWidget {
        private Props textualPropsSnapshot;
        private Runnable latestDeclaredDeny;
        private Runnable latestDeclaredConfirm;
        private FConfirmPopupWidget popupWidget;

        PopupHost(Props overlayProps) {
            this.textualPropsSnapshot = overlayProps.textualCopy();
            this.latestDeclaredDeny = overlayProps.onDeny();
            this.latestDeclaredConfirm = overlayProps.onConfirm();
            this.popupWidget = new FConfirmPopupWidget(textualPropsSnapshot.title(), textualPropsSnapshot.message(),
                    textualPropsSnapshot.confirmLabel(), textualPropsSnapshot.denyLabel(),
                    FConfirmPopupWidget.ConfirmStyle.DESTRUCTIVE, this::bridgeDeny, this::bridgeConfirm);
            addChild(popupWidget);
        }

        PopupHost refresh(Props nextDeclaredProps) {
            Props nextTextual = nextDeclaredProps.textualCopy();
            latestDeclaredDeny = nextDeclaredProps.onDeny();
            latestDeclaredConfirm = nextDeclaredProps.onConfirm();
            if (!nextTextual.equals(textualPropsSnapshot)) {
                textualPropsSnapshot = nextTextual;
                popupWidget = new FConfirmPopupWidget(textualPropsSnapshot.title(), textualPropsSnapshot.message(),
                        textualPropsSnapshot.confirmLabel(), textualPropsSnapshot.denyLabel(),
                        FConfirmPopupWidget.ConfirmStyle.DESTRUCTIVE, this::bridgeDeny, this::bridgeConfirm);
                clearChildren();
                addChild(popupWidget);
            }
            return this;
        }

        private void bridgeDeny() {
            if (latestDeclaredDeny != null) {
                latestDeclaredDeny.run();
            }
        }

        private void bridgeConfirm() {
            if (latestDeclaredConfirm != null) {
                latestDeclaredConfirm.run();
            }
        }

        @Override
        public boolean wantsPointer() {
            return true;
        }

        @Override
        public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
            setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
            float dockPanelLogicalWidth = Math.min(PANEL_WIDTH, layoutWidth);
            float dockPanelLogicalX = layoutX + layoutWidth - dockPanelLogicalWidth;
            FConfirmPopupWidget popup = (FConfirmPopupWidget) childrenView().get(0);
            popup.layout(measure, dockPanelLogicalX, layoutY, dockPanelLogicalWidth, layoutHeight);
        }

        @Override
        public boolean mouseDown(float pointerX, float pointerY, int button) {
            FConfirmPopupWidget popup = (FConfirmPopupWidget) childrenView().get(0);
            return popup.mouseDown(pointerX, pointerY, button);
        }
    }
}
