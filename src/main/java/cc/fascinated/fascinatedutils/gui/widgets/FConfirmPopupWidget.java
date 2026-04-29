package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.TextOverflow;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;

/**
 * A generic confirm/deny popup that can be used for any two-option decision.
 *
 * <p>The confirm button is styled according to the supplied {@link ConfirmStyle}:
 * {@link ConfirmStyle#NEUTRAL} uses the accent-purple style for a positive action,
 * while {@link ConfirmStyle#DESTRUCTIVE} uses red to signal a dangerous action.</p>
 */
public class FConfirmPopupWidget extends FPopupWidget {

    /**
     * Visual style applied to the confirm button.
     */
    public enum ConfirmStyle {
        NEUTRAL,
        DESTRUCTIVE
    }

    private final FLabelWidget titleLabel;
    private final FLabelWidget messageLabel;
    private final FButtonWidget denyButton;
    private final FButtonWidget confirmButton;

    /**
     * Creates a confirm/deny popup.
     *
     * @param title        required heading shown at the top of the dialog
     * @param message      optional body text; pass {@code null} or blank to omit
     * @param confirmLabel label for the confirm/accept button
     * @param denyLabel    label for the deny/cancel button
     * @param confirmStyle visual style for the confirm button
     * @param onDeny       callback fired when the user denies, cancels, or dismisses
     * @param onConfirm    callback fired when the user confirms
     */
    public FConfirmPopupWidget(String title, String message, String confirmLabel, String denyLabel,
                               ConfirmStyle confirmStyle, Runnable onDeny, Runnable onConfirm) {
        super(onDeny);

        titleLabel = new FLabelWidget();
        titleLabel.setText(title);
        titleLabel.setAlignX(Align.START);
        titleLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textPrimary());
        titleLabel.setTextBold(true);

        boolean hasMessage = message != null && !message.isBlank();
        if (hasMessage) {
            messageLabel = new FLabelWidget();
            messageLabel.setText(message);
            messageLabel.setAlignX(Align.START);
            messageLabel.setOverflow(TextOverflow.WRAP);
            messageLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
        } else {
            messageLabel = null;
        }

        denyButton = new FButtonWidget(onDeny, () -> denyLabel, 100f, 1, 2f, 8f, 1f, 8f);

        if (confirmStyle == ConfirmStyle.DESTRUCTIVE) {
            confirmButton = new FButtonWidget(onConfirm, () -> confirmLabel, 100f, 1, 2f, 8f, 1f, 8f) {
                @Override
                protected int resolveButtonFillColorArgb(boolean hovered) {
                    return hovered ? 0xFF7E2E2E : 0xFF692727;
                }

                @Override
                protected int resolveButtonBorderColorArgb(boolean hovered) {
                    return hovered ? 0xFFFF8A8A : 0xFFE07171;
                }
            };
        } else {
            confirmButton = new FButtonWidget(onConfirm, () -> confirmLabel, 100f, 1, 2f, 8f, 1f, 8f) {
                @Override
                protected int resolveButtonFillColorArgb(boolean hovered) {
                    return hovered ? 0xFF3D2B7A : 0xFF2E2260;
                }

                @Override
                protected int resolveButtonBorderColorArgb(boolean hovered) {
                    return hovered ? 0xFF9470D0 : 0xFF7C5CBF;
                }
            };
        }

        addChild(titleLabel);
        if (messageLabel != null) {
            addChild(messageLabel);
        }
        addChild(denyButton);
        addChild(confirmButton);
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        float popupWidth = Math.min(Math.max(240f, layoutWidth * 0.45f), 360f);
        float horizontalPadding = UITheme.PADDING_MD;
        float verticalPadding = UITheme.PADDING_MD;
        float rowGap = UITheme.GAP_SM;
        float bodyWidth = Math.max(0f, popupWidth - 2f * horizontalPadding);

        float titleHeight = titleLabel.intrinsicHeightForColumn(measure, bodyWidth);
        float messageHeight = messageLabel != null ? messageLabel.intrinsicHeightForColumn(measure, bodyWidth) : 0f;
        float actionsHeight = denyButton.intrinsicHeightForColumn(measure, bodyWidth);

        float computedDialogHeight = verticalPadding + titleHeight
                + (messageHeight > 0f ? rowGap + messageHeight : 0f)
                + rowGap + actionsHeight + verticalPadding;
        float popupHeight = Math.max(90f, computedDialogHeight);
        setDialogBounds(layoutX, layoutY, layoutWidth, layoutHeight, popupWidth, popupHeight);

        float cursorY = dialogY() + verticalPadding;
        titleLabel.layout(measure, dialogX() + horizontalPadding, cursorY, bodyWidth, titleHeight);
        cursorY += titleHeight;

        if (messageLabel != null) {
            cursorY += rowGap;
            messageLabel.layout(measure, dialogX() + horizontalPadding, cursorY, bodyWidth, messageHeight);
        }

        float actionsY = dialogY() + dialogHeight() - verticalPadding - actionsHeight;
        float actionGap = UITheme.GAP_SM;
        float actionWidth = Math.max(0f, (bodyWidth - actionGap) * 0.5f);
        denyButton.layout(measure, dialogX() + horizontalPadding, actionsY, actionWidth,
                denyButton.intrinsicHeightForColumn(measure, actionWidth));
        confirmButton.layout(measure, dialogX() + horizontalPadding + actionWidth + actionGap, actionsY, actionWidth,
                confirmButton.intrinsicHeightForColumn(measure, actionWidth));
    }
}
