package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.GuiFocusState;
import cc.fascinated.fascinatedutils.gui.core.TextOverflow;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FLabelWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FPopupWidget;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import net.minecraft.network.chat.Component;

public class FProfileRenamePopupWidget extends FPopupWidget {
    private static final int PROFILE_NAME_INPUT_FOCUS_ID = 5202;
    private final String currentProfileName;
    private final Runnable onCancel;
    private final SubmitRenameCallback onSubmit;
    private final FLabelWidget titleLabel;
    private final FLabelWidget descriptionLabel;
    private final FOutlinedTextInputWidget profileNameInput;
    private final FLabelWidget validationLabel;
    private final FButtonWidget cancelButton;
    private final FButtonWidget renameButton;
    private String newName;
    private String validationMessage = "";

    public FProfileRenamePopupWidget(String currentName, Runnable onCancel, SubmitRenameCallback onSubmit) {
        super(onCancel);
        this.currentProfileName = currentName;
        this.onCancel = onCancel;
        this.onSubmit = onSubmit;
        this.newName = currentName;

        titleLabel = new FLabelWidget();
        titleLabel.setText(Component.translatable("fascinatedutils.setting.shell.profile_rename_popup_title").getString());
        titleLabel.setAlignX(Align.START);
        titleLabel.setOverflow(TextOverflow.WRAP);
        titleLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textPrimary());

        descriptionLabel = new FLabelWidget();
        descriptionLabel.setText(Component.translatable("fascinatedutils.setting.shell.profile_rename_popup_description").getString());
        descriptionLabel.setAlignX(Align.START);
        descriptionLabel.setOverflow(TextOverflow.WRAP);
        descriptionLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());

        profileNameInput = new FOutlinedTextInputWidget(PROFILE_NAME_INPUT_FOCUS_ID, 64, 24f, () -> "");
        profileNameInput.setValue(currentName);
        profileNameInput.setOnChange(value -> {
            newName = value;
            refreshValidationState();
        });
        profileNameInput.setExternalFocusIdSupplier(GuiFocusState::getFocusedId);

        validationLabel = new FLabelWidget();
        validationLabel.setAlignX(Align.START);
        validationLabel.setOverflow(TextOverflow.WRAP);
        validationLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textAccent());

        cancelButton = new FButtonWidget(onCancel, () -> Component.translatable("fascinatedutils.setting.shell.profile_popup_cancel").getString(), 100f, 1, 2f, 8f, 1f, 8f);
        renameButton = new FButtonWidget(this::submit, () -> Component.translatable("fascinatedutils.setting.shell.profile_rename_popup_confirm").getString(), 100f, 1, 2f, 8f, 1f, 8f) {
            @Override
            protected int resolveButtonBorderColorArgb(boolean hovered) {
                if (!isInputValid()) {
                    return FascinatedGuiTheme.INSTANCE.borderMuted();
                }
                return super.resolveButtonBorderColorArgb(hovered);
            }
        };

        addChild(titleLabel);
        addChild(descriptionLabel);
        addChild(profileNameInput);
        addChild(validationLabel);
        addChild(cancelButton);
        addChild(renameButton);

        refreshValidationState();
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        float popupWidth = Math.min(Math.max(240f, layoutWidth * 0.45f), 360f);
        float horizontalPadding = UITheme.PADDING_MD;
        float verticalPadding = UITheme.PADDING_MD;
        float rowGap = UITheme.GAP_SM;
        float bodyWidth = Math.max(0f, popupWidth - 2f * horizontalPadding);
        float titleHeight = titleLabel.intrinsicHeightForColumn(measure, bodyWidth);
        float descriptionHeight = descriptionLabel.intrinsicHeightForColumn(measure, bodyWidth);
        float inputHeight = profileNameInput.intrinsicHeightForColumn(measure, bodyWidth);
        boolean hasValidationMessage = !validationMessage.isEmpty();
        float validationHeight = 0f;
        if (hasValidationMessage) {
            validationHeight = validationLabel.intrinsicHeightForColumn(measure, bodyWidth);
        }
        float actionsHeight = cancelButton.intrinsicHeightForColumn(measure, bodyWidth);

        float computedDialogHeight = verticalPadding + titleHeight + rowGap + descriptionHeight + rowGap + inputHeight + (hasValidationMessage ? rowGap + validationHeight : 0f) + rowGap + actionsHeight + verticalPadding;
        float popupHeight = Math.max(136f, computedDialogHeight);
        setDialogBounds(layoutX, layoutY, layoutWidth, layoutHeight, popupWidth, popupHeight);

        float cursorY = dialogY() + verticalPadding;
        titleLabel.layout(measure, dialogX() + horizontalPadding, cursorY, bodyWidth, titleHeight);
        cursorY += titleHeight + rowGap;

        descriptionLabel.layout(measure, dialogX() + horizontalPadding, cursorY, bodyWidth, descriptionHeight);
        cursorY += descriptionHeight + rowGap;

        profileNameInput.layout(measure, dialogX() + horizontalPadding, cursorY, bodyWidth, inputHeight);
        cursorY += inputHeight;

        if (hasValidationMessage) {
            cursorY += rowGap;
            validationLabel.layout(measure, dialogX() + horizontalPadding, cursorY, bodyWidth, validationHeight);
        }

        float actionsY = dialogY() + dialogHeight() - verticalPadding - actionsHeight;
        float actionGap = UITheme.GAP_SM;
        float actionWidth = Math.max(0f, (bodyWidth - actionGap) * 0.5f);
        cancelButton.layout(measure, dialogX() + horizontalPadding, actionsY, actionWidth, cancelButton.intrinsicHeightForColumn(measure, actionWidth));
        renameButton.layout(measure, dialogX() + horizontalPadding + actionWidth + actionGap, actionsY, actionWidth, renameButton.intrinsicHeightForColumn(measure, actionWidth));
    }

    private void submit() {
        if (!isInputValid()) {
            return;
        }
        onSubmit.renameProfile(newName.trim());
    }

    private boolean isInputValid() {
        return validationMessage.isEmpty();
    }

    private void refreshValidationState() {
        validationMessage = resolveValidationMessage(newName);
        validationLabel.setText(validationMessage);
    }

    private String resolveValidationMessage(String requestedName) {
        String normalizedName = requestedName == null ? "" : requestedName.trim();
        if (normalizedName.isEmpty()) {
            return Component.translatable("fascinatedutils.setting.shell.profile_name_error_empty").getString();
        }
        String normalizedCurrentName = currentProfileName == null ? "" : currentProfileName.trim();
        if (normalizedName.equalsIgnoreCase(normalizedCurrentName)) {
            return "";
        }
        if (ModConfig.profiles().profileNameExists(normalizedName)) {
            return Component.translatable("fascinatedutils.setting.shell.profile_name_error_duplicate_rename").getString();
        }
        return "";
    }

    @FunctionalInterface
    public interface SubmitRenameCallback {
        void renameProfile(String newName);
    }
}
