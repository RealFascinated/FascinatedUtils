package cc.fascinated.fascinatedutils.gui.waypoints;

import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.GuiFocusState;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FLabelWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FPopupWidget;
import net.minecraft.network.chat.Component;

public class WaypointRenamePopupWidget extends FPopupWidget {
    private static final int INPUT_FOCUS_ID = 6201;

    private final RenameCallback onSubmit;
    private final FLabelWidget titleLabel;
    private final FOutlinedTextInputWidget nameInput;
    private final FButtonWidget cancelButton;
    private final FButtonWidget renameButton;
    private String newName;

    public WaypointRenamePopupWidget(String currentName, Runnable onCancel, RenameCallback onSubmit) {
        super(onCancel);
        this.onSubmit = onSubmit;
        this.newName = currentName;

        titleLabel = new FLabelWidget();
        titleLabel.setText(Component.translatable("fascinatedutils.waypoints.rename_popup.title").getString());
        titleLabel.setAlignX(Align.START);
        titleLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textPrimary());

        nameInput = new FOutlinedTextInputWidget(INPUT_FOCUS_ID, 64, 24f, () -> "");
        nameInput.setValue(currentName);
        nameInput.setOnChange(value -> newName = value);
        nameInput.setExternalFocusIdSupplier(GuiFocusState::getFocusedId);

        cancelButton = new FButtonWidget(onCancel, () -> Component.translatable("fascinatedutils.waypoints.popup.cancel").getString(), 100f, 1, 2f, 8f, 1f, 8f);
        renameButton = new FButtonWidget(this::submit, () -> Component.translatable("fascinatedutils.waypoints.rename_popup.confirm").getString(), 100f, 1, 2f, 8f, 1f, 8f);

        addChild(titleLabel);
        addChild(nameInput);
        addChild(cancelButton);
        addChild(renameButton);
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        float popupWidth = Math.min(Math.max(240f, layoutWidth * 0.45f), 360f);
        float horizontalPadding = UITheme.PADDING_MD;
        float verticalPadding = UITheme.PADDING_MD;
        float rowGap = UITheme.GAP_SM;
        float bodyWidth = Math.max(0f, popupWidth - 2f * horizontalPadding);
        float titleHeight = titleLabel.intrinsicHeightForColumn(measure, bodyWidth);
        float inputHeight = nameInput.intrinsicHeightForColumn(measure, bodyWidth);
        float actionsHeight = cancelButton.intrinsicHeightForColumn(measure, bodyWidth);

        float computedHeight = verticalPadding + titleHeight + rowGap + inputHeight + rowGap + actionsHeight + verticalPadding;
        setDialogBounds(layoutX, layoutY, layoutWidth, layoutHeight, popupWidth, Math.max(120f, computedHeight));

        float cursorY = dialogY() + verticalPadding;
        titleLabel.layout(measure, dialogX() + horizontalPadding, cursorY, bodyWidth, titleHeight);
        cursorY += titleHeight + rowGap;
        nameInput.layout(measure, dialogX() + horizontalPadding, cursorY, bodyWidth, inputHeight);

        float actionsY = dialogY() + dialogHeight() - verticalPadding - actionsHeight;
        float actionGap = UITheme.GAP_SM;
        float actionWidth = Math.max(0f, (bodyWidth - actionGap) / 2f);
        cancelButton.layout(measure, dialogX() + horizontalPadding, actionsY, actionWidth, actionsHeight);
        renameButton.layout(measure, dialogX() + horizontalPadding + actionWidth + actionGap, actionsY, actionWidth, actionsHeight);
    }

    private void submit() {
        String trimmed = newName == null ? "" : newName.trim();
        if (!trimmed.isEmpty()) {
            onSubmit.rename(trimmed);
        }
    }

    @FunctionalInterface
    public interface RenameCallback {
        void rename(String newName);
    }
}
