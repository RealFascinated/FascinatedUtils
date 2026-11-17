package cc.fascinated.fascinatedutils.gui.node.popup;

import cc.fascinated.fascinatedutils.gui.node.control.ButtonNode;

import cc.fascinated.fascinatedutils.gui.node.display.TextNode;

import cc.fascinated.fascinatedutils.gui.components.UiTranslations;
import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * A popup with a centred title, a column of content and a cancel and confirm button row. Dismissing the popup, by
 * clicking outside or pressing Escape, counts as cancelling. The popup sizes itself to its content between a minimum
 * that fits the buttons and a maximum width.
 *
 * @param <T> the concrete popup type, returned from fluent setters
 */
public abstract class DialogPopupNode<T extends DialogPopupNode<T>> extends PopupNode<T> {
    protected static final int PAD_SIDE = 16;
    protected static final int PAD_TOP = 16;
    protected static final int PAD_BOTTOM = 12;
    protected static final int BODY_GAP = 6;
    private static final int BODY_BUTTON_GAP = 10;
    private static final int BUTTON_GAP = 8;
    private static final int MIN_WIDTH = UiMetrics.DIALOG_BUTTON_WIDTH * 2 + BUTTON_GAP + PAD_SIDE * 2;
    private static final int MAX_WIDTH = 320;

    private Supplier<String> titleSupplier = () -> "";
    private Supplier<String> confirmLabelSupplier = () -> UiTranslations.INSTANCE.get("alumite.common.confirm");
    private Runnable onCancel = () -> {};
    private Runnable onConfirm = () -> {};

    private final PositionedNode<?> body = new PositionedNode<>().left(PAD_SIDE).right(PAD_SIDE).top(PAD_TOP).columnGap(BODY_GAP);
    private final ButtonNode confirmButton;

    protected DialogPopupNode() {
        setOnDismiss(() -> onCancel.run());

        TextNode titleNode = new TextNode(() -> titleSupplier.get());
        titleNode.setBold(true).setColorResolver(UiTheme::textPrimary).alignX(0.5f);
        body.addChild(titleNode);
        addChild(body);

        ButtonNode cancelButton = new ButtonNode(() -> UiTranslations.INSTANCE.get("alumite.common.cancel")).setOnPress(() -> onCancel.run());
        cancelButton.size(UiMetrics.DIALOG_BUTTON_WIDTH, UiMetrics.CONTROL_HEIGHT);
        confirmButton = new ButtonNode(() -> confirmLabelSupplier.get()).setOnPress(() -> onConfirm.run());
        confirmButton.size(UiMetrics.DIALOG_BUTTON_WIDTH, UiMetrics.CONTROL_HEIGHT);

        PositionedNode<?> buttonRow = new PositionedNode<>().size(UiMetrics.DIALOG_BUTTON_WIDTH * 2 + BUTTON_GAP, UiMetrics.CONTROL_HEIGHT).rowGap(BUTTON_GAP).alignX(0.5f).bottom(PAD_BOTTOM);
        buttonRow.addChild(cancelButton);
        buttonRow.addChild(confirmButton);
        addChild(buttonRow);
    }

    /**
     * Sets the heading.
     *
     * @param titleSupplier supplies the heading text each frame
     * @return this popup
     */
    public T setTitle(Supplier<String> titleSupplier) {
        this.titleSupplier = titleSupplier == null ? () -> "" : titleSupplier;
        return self();
    }

    /**
     * Sets a fixed heading.
     *
     * @param title the heading text
     * @return this popup
     */
    public T setTitle(String title) {
        String text = title == null ? "" : title;
        return setTitle(() -> text);
    }

    /**
     * Sets the confirm button label.
     *
     * @param confirmLabelSupplier supplies the label each frame; null restores the default "Confirm"
     * @return this popup
     */
    public T setConfirmLabel(Supplier<String> confirmLabelSupplier) {
        this.confirmLabelSupplier = confirmLabelSupplier == null ? () -> UiTranslations.INSTANCE.get("alumite.common.confirm") : confirmLabelSupplier;
        return self();
    }

    /**
     * Sets a fixed confirm button label.
     *
     * @param confirmLabel the label; null restores the default "Confirm"
     * @return this popup
     */
    public T setConfirmLabel(String confirmLabel) {
        return setConfirmLabel(confirmLabel == null ? null : () -> confirmLabel);
    }

    /**
     * Sets the confirm button label colour from the active theme each frame.
     *
     * @param colorResolver resolves the colour; null restores the default
     * @return this popup
     */
    public T setConfirmLabelColorResolver(ToIntFunction<UiTheme> colorResolver) {
        confirmButton.setLabelColorResolver(colorResolver);
        return self();
    }

    /**
     * Sets when the confirm button is disabled.
     *
     * @param confirmDisabled supplies whether confirming is currently not allowed; null means always allowed
     * @return this popup
     */
    public T setConfirmDisabled(BooleanSupplier confirmDisabled) {
        confirmButton.setDisabled(confirmDisabled);
        return self();
    }

    /**
     * Sets what happens when the user cancels, clicks the cancel button or dismisses the popup.
     *
     * @param onCancel the action; null does nothing
     * @return this popup
     */
    public T setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel == null ? () -> {} : onCancel;
        return self();
    }

    /**
     * Sets what happens when the user presses the confirm button.
     *
     * @param onConfirm the action; null does nothing
     * @return this popup
     */
    public T setOnConfirm(Runnable onConfirm) {
        this.onConfirm = onConfirm == null ? () -> {} : onConfirm;
        return self();
    }

    /**
     * Adds content below the title.
     *
     * @param contentNode the node to stack below the existing content
     */
    protected void addContent(UiNode contentNode) {
        body.addChild(contentNode);
    }

    /**
     * The width the content would like, excluding the side padding. Defaults to the title's width.
     *
     * @param renderFrame the active renderer, used to measure text
     * @return the preferred content width in pixels
     */
    protected int preferredContentWidth(AlumiteGuiRenderer renderFrame) {
        return renderFrame.measureTextWidth(titleSupplier.get(), true);
    }

    @Override
    protected void configurePopup(AlumiteGuiRenderer renderFrame, int availableWidth, int availableHeight) {
        int width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, preferredContentWidth(renderFrame) + PAD_SIDE * 2));
        if (availableWidth > 0) {
            width = Math.min(width, availableWidth);
        }
        int bodyHeight = body.measure(renderFrame, width, availableHeight).height();
        setPopupWidth(width);
        setPopupHeight(PAD_TOP + bodyHeight + BODY_BUTTON_GAP + UiMetrics.CONTROL_HEIGHT + PAD_BOTTOM);
    }
}
