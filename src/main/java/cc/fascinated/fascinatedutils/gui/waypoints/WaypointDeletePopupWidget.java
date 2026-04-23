package cc.fascinated.fascinatedutils.gui.waypoints;

import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.TextOverflow;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FLabelWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FPopupWidget;
import net.minecraft.network.chat.Component;

public class WaypointDeletePopupWidget extends FPopupWidget {
    private final FLabelWidget titleLabel;
    private final FLabelWidget messageLabel;
    private final FButtonWidget cancelButton;
    private final FButtonWidget deleteButton;

    public WaypointDeletePopupWidget(String waypointName, Runnable onCancel, Runnable onConfirm) {
        super(onCancel);

        titleLabel = new FLabelWidget();
        titleLabel.setText(Component.translatable("fascinatedutils.waypoints.delete_popup.title").getString());
        titleLabel.setAlignX(Align.START);
        titleLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textPrimary());

        messageLabel = new FLabelWidget();
        messageLabel.setText(Component.translatable("fascinatedutils.waypoints.delete_popup.message", waypointName).getString());
        messageLabel.setAlignX(Align.START);
        messageLabel.setOverflow(TextOverflow.WRAP);
        messageLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());

        cancelButton = new FButtonWidget(onCancel, () -> Component.translatable("fascinatedutils.waypoints.popup.cancel").getString(), 100f, 1, 2f, 8f, 1f, 8f);
        deleteButton = new FButtonWidget(onConfirm, () -> Component.translatable("fascinatedutils.waypoints.delete_popup.confirm").getString(), 100f, 1, 2f, 8f, 1f, 8f) {
            @Override
            protected int resolveButtonFillColorArgb(boolean hovered) {
                return hovered ? 0xFF7E2E2E : 0xFF692727;
            }

            @Override
            protected int resolveButtonBorderColorArgb(boolean hovered) {
                return hovered ? 0xFFFF8A8A : 0xFFE07171;
            }
        };

        addChild(titleLabel);
        addChild(messageLabel);
        addChild(cancelButton);
        addChild(deleteButton);
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        float popupWidth = Math.min(Math.max(240f, layoutWidth * 0.45f), 360f);
        float horizontalPadding = UITheme.PADDING_MD;
        float verticalPadding = UITheme.PADDING_MD;
        float rowGap = UITheme.GAP_SM;
        float bodyWidth = Math.max(0f, popupWidth - 2f * horizontalPadding);
        float titleHeight = titleLabel.intrinsicHeightForColumn(measure, bodyWidth);
        float messageHeight = messageLabel.intrinsicHeightForColumn(measure, bodyWidth);
        float actionsHeight = cancelButton.intrinsicHeightForColumn(measure, bodyWidth);

        float computedHeight = verticalPadding + titleHeight + rowGap + messageHeight + rowGap + actionsHeight + verticalPadding;
        setDialogBounds(layoutX, layoutY, layoutWidth, layoutHeight, popupWidth, Math.max(110f, computedHeight));

        float cursorY = dialogY() + verticalPadding;
        titleLabel.layout(measure, dialogX() + horizontalPadding, cursorY, bodyWidth, titleHeight);
        cursorY += titleHeight + rowGap;
        messageLabel.layout(measure, dialogX() + horizontalPadding, cursorY, bodyWidth, messageHeight);

        float actionsY = dialogY() + dialogHeight() - verticalPadding - actionsHeight;
        float actionGap = UITheme.GAP_SM;
        float actionWidth = Math.max(0f, (bodyWidth - actionGap) / 2f);
        cancelButton.layout(measure, dialogX() + horizontalPadding, actionsY, actionWidth, actionsHeight);
        deleteButton.layout(measure, dialogX() + horizontalPadding + actionWidth + actionGap, actionsY, actionWidth, actionsHeight);
    }
}
