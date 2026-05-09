package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;

import java.util.function.Function;

/**
 * Popup dialog with a title, optional message body, and two action buttons (cancel + confirm).
 *
 * <p>The confirm button label and color are configurable so this can be used for both
 * neutral confirmations and destructive actions (e.g. show the confirm label in red).</p>
 *
 * <pre>{@code
 * new ConfirmPopupNode()
 *     .setTitle("Delete message?")
 *     .setMessage("This cannot be undone.")
 *     .setConfirmLabel("Delete")
 *     .setConfirmLabelColorArgb(0xFFFF5555)
 *     .setOnCancel(() -> state.set(false))
 *     .setOnConfirm(() -> { state.set(false); doDelete(); });
 * }</pre>
 */
public class ConfirmPopupNode extends PopupNode {

    private static final int POPUP_WIDTH = 220;
    private static final int POPUP_HEIGHT = 90;
    private static final int PAD_TOP = 16;
    private static final int PAD_BOTTOM = 12;
    private static final int MESSAGE_TITLE_GAP = 6;
    private static final int PAD_HORIZONTAL = 20;
    private static final int BUTTON_HEIGHT = 22;
    private static final int BUTTON_GAP = 8;
    private static final int BUTTON_WIDTH = (POPUP_WIDTH - PAD_HORIZONTAL * 2 - BUTTON_GAP) / 2;

    private String title = "";
    private String message = null;
    private String cancelLabel = "Cancel";
    private String confirmLabel = "Confirm";
    private Runnable onCancel = () -> {};
    private Runnable onConfirm = () -> {};

    private final ButtonNode cancelButton;
    private final ButtonNode confirmButton;

    public ConfirmPopupNode() {
        setPopupWidth(POPUP_WIDTH);
        setPopupHeight(POPUP_HEIGHT);
        setOnDismiss(() -> onCancel.run());

        cancelButton = new ButtonNode(cancelLabel).setOnPress(() -> onCancel.run());
        confirmButton = new ButtonNode(confirmLabel).setOnPress(() -> onConfirm.run());

        addPopupChild(cancelButton);
        addPopupChild(confirmButton);
    }

    public ConfirmPopupNode setTitle(String title) {
        this.title = title == null ? "" : title;
        return this;
    }

    public ConfirmPopupNode setMessage(String message) {
        this.message = message;
        return this;
    }

    public ConfirmPopupNode setCancelLabel(String cancelLabel) {
        this.cancelLabel = cancelLabel == null ? "Cancel" : cancelLabel;
        cancelButton.setLabel(this.cancelLabel);
        return this;
    }

    public ConfirmPopupNode setConfirmLabel(String confirmLabel) {
        this.confirmLabel = confirmLabel == null ? "Confirm" : confirmLabel;
        confirmButton.setLabel(this.confirmLabel);
        return this;
    }

    public ConfirmPopupNode setConfirmLabelColorArgb(Integer confirmLabelColorArgb) {
        confirmButton.setLabelColorArgb(confirmLabelColorArgb);
        return this;
    }

    public ConfirmPopupNode setConfirmLabelColorResolver(Function<UiTheme, Integer> colorResolver) {
        confirmButton.setLabelColorArgb(colorResolver != null ? colorResolver.apply(UiThemeRepository.get()) : null);
        return this;
    }

    public ConfirmPopupNode setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel == null ? () -> {} : onCancel;
        setOnDismiss(this.onCancel);
        cancelButton.setOnPress(this.onCancel);
        return this;
    }

    public ConfirmPopupNode setOnConfirm(Runnable onConfirm) {
        this.onConfirm = onConfirm == null ? () -> {} : onConfirm;
        confirmButton.setOnPress(this.onConfirm);
        return this;
    }

    @Override
    public void layout(RenderFrame renderFrame, int parentX, int parentY, int parentWidth, int parentHeight) {
        // Recompute popup height based on whether message is shown
        int computedHeight = message != null && !message.isBlank()
                ? POPUP_HEIGHT + renderFrame.fontHeight() + MESSAGE_TITLE_GAP
                : POPUP_HEIGHT;
        setPopupHeight(computedHeight);

        super.layout(renderFrame, parentX, parentY, parentWidth, parentHeight);

        int buttonY = popupY() + popupHeight() - PAD_BOTTOM - BUTTON_HEIGHT;
        int buttonStartX = popupX() + PAD_HORIZONTAL;

        cancelButton.layout(renderFrame, buttonStartX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);
        confirmButton.layout(renderFrame, buttonStartX + BUTTON_WIDTH + BUTTON_GAP, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        super.renderSelf(renderFrame, deltaSeconds);

        int centerX = popupX() + popupWidth() / 2;

        // Title
        if (!title.isBlank()) {
            int titleWidth = renderFrame.measureTextWidth(title, true);
            renderFrame.drawText(title, centerX - titleWidth / 2, popupY() + PAD_TOP, renderFrame.theme().textPrimary(), false, true);
        }

        // Message
        if (message != null && !message.isBlank()) {
            int messageY = popupY() + PAD_TOP + renderFrame.fontHeight() + MESSAGE_TITLE_GAP;
            int messageWidth = renderFrame.measureTextWidth(message, false);
            renderFrame.drawText(message, centerX - messageWidth / 2, messageY, renderFrame.theme().textMuted(), false, false);
        }
    }
}
