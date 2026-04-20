package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.GuiFocusState;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.core.TextOverflow;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.*;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import net.minecraft.network.chat.Component;

public class ProfileCreatePopupWidget extends FPopupWidget {
    private static final int PROFILE_NAME_INPUT_FOCUS_ID = 5201;
    private final Ref<String> profileNameRef;
    private final Ref<Boolean> copyDefaultProfileSettingsRef;
    private final Runnable onCancel;
    private final SubmitProfileCallback onSubmit;
    private final FLabelWidget titleLabel;
    private final FLabelWidget descriptionLabel;
    private final FOutlinedTextInputWidget profileNameInput;
    private final FLabelWidget validationLabel;
    private final FIconCheckboxWidget copyDefaultToggleCheckbox;
    private final FButtonWidget cancelButton;
    private final FButtonWidget createButton;
    private String validationMessage = "";

    public ProfileCreatePopupWidget(Ref<String> profileNameRef, Ref<Boolean> copyDefaultProfileSettingsRef, Runnable onCancel, SubmitProfileCallback onSubmit) {
        super(onCancel);
        this.profileNameRef = profileNameRef;
        this.copyDefaultProfileSettingsRef = copyDefaultProfileSettingsRef;
        this.onCancel = onCancel;
        this.onSubmit = onSubmit;

        titleLabel = new FLabelWidget();
        titleLabel.setText(Component.translatable("fascinatedutils.setting.shell.profile_popup_title").getString());
        titleLabel.setAlignX(Align.START);
        titleLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textPrimary());

        descriptionLabel = new FLabelWidget();
        descriptionLabel.setText(Component.translatable("fascinatedutils.setting.shell.profile_popup_description").getString());
        descriptionLabel.setAlignX(Align.START);
        descriptionLabel.setOverflow(TextOverflow.WRAP);
        descriptionLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());

        profileNameInput = new FOutlinedTextInputWidget(PROFILE_NAME_INPUT_FOCUS_ID, 45, 17f, () -> Component.translatable("fascinatedutils.setting.shell.profile_name_placeholder").getString());
        profileNameInput.setValue(profileNameRef.getValue() == null ? "" : profileNameRef.getValue());
        profileNameInput.setOnChange(value -> {
            profileNameRef.setValue(value);
            refreshValidationState();
        });
        profileNameInput.setExternalFocusIdSupplier(GuiFocusState::getFocusedId);

        validationLabel = new FLabelWidget();
        validationLabel.setAlignX(Align.START);
        validationLabel.setOverflow(TextOverflow.WRAP);
        validationLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textAccent());

        copyDefaultToggleCheckbox = new FIconCheckboxWidget(Boolean.TRUE.equals(copyDefaultProfileSettingsRef.getValue()), checked -> copyDefaultProfileSettingsRef.setValue(checked), () -> Boolean.TRUE.equals(copyDefaultProfileSettingsRef.getValue()) ? Component.translatable("fascinatedutils.setting.shell.profile_popup_copy_default_on").getString() : Component.translatable("fascinatedutils.setting.shell.profile_popup_copy_default_off").getString(), 154f);

        cancelButton = new FButtonWidget(onCancel, () -> Component.translatable("fascinatedutils.setting.shell.profile_popup_cancel").getString(), 70f, 1, 1f, 6f, 1f, 6f);
        createButton = new FButtonWidget(this::submit, () -> Component.translatable("fascinatedutils.setting.shell.profile_popup_create").getString(), 70f, 1, 1f, 6f, 1f, 6f) {
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
        addChild(copyDefaultToggleCheckbox);
        addChild(cancelButton);
        addChild(createButton);

        refreshValidationState();
    }

    private static String resolveValidationMessage(String requestedName) {
        String normalizedName = requestedName == null ? "" : requestedName.trim();
        if (normalizedName.isEmpty()) {
            return Component.translatable("fascinatedutils.setting.shell.profile_name_error_empty").getString();
        }
        if (ModConfig.profileNameExists(normalizedName)) {
            return Component.translatable("fascinatedutils.setting.shell.profile_name_error_duplicate_create").getString();
        }
        return "";
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        float popupWidth = Math.min(Math.max(168f, layoutWidth * 0.45f), 252f);
        float horizontalPadding = 7f;
        float verticalPadding = 7f;
        float rowGap = 3f;
        float bodyWidth = Math.max(0f, popupWidth - 2f * horizontalPadding);
        float titleHeight = titleLabel.intrinsicHeightForColumn(measure, bodyWidth);
        float descriptionHeight = descriptionLabel.intrinsicHeightForColumn(measure, bodyWidth);
        float inputHeight = profileNameInput.intrinsicHeightForColumn(measure, bodyWidth);
        boolean hasValidationMessage = !validationMessage.isEmpty();
        float validationHeight = 0f;
        if (hasValidationMessage) {
            validationHeight = validationLabel.intrinsicHeightForColumn(measure, bodyWidth);
        }
        float toggleHeight = copyDefaultToggleCheckbox.intrinsicHeightForColumn(measure, bodyWidth);
        float actionsHeight = cancelButton.intrinsicHeightForColumn(measure, bodyWidth);

        float computedDialogHeight = verticalPadding + titleHeight + rowGap + descriptionHeight + rowGap + inputHeight + (hasValidationMessage ? rowGap + validationHeight : 0f) + rowGap + toggleHeight + rowGap + actionsHeight + verticalPadding;
        float popupHeight = Math.max(130f, computedDialogHeight);
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
            cursorY += validationHeight;
        }

        cursorY += rowGap;

        copyDefaultToggleCheckbox.setChecked(Boolean.TRUE.equals(copyDefaultProfileSettingsRef.getValue()));
        copyDefaultToggleCheckbox.setOuterWidth(bodyWidth);
        copyDefaultToggleCheckbox.layout(measure, dialogX() + horizontalPadding, cursorY, bodyWidth, toggleHeight);

        float actionsY = dialogY() + dialogHeight() - verticalPadding - actionsHeight;
        float actionGap = 3f;
        float actionWidth = Math.max(0f, (bodyWidth - actionGap) * 0.5f);
        cancelButton.layout(measure, dialogX() + horizontalPadding, actionsY, actionWidth, cancelButton.intrinsicHeightForColumn(measure, actionWidth));
        createButton.layout(measure, dialogX() + horizontalPadding + actionWidth + actionGap, actionsY, actionWidth, createButton.intrinsicHeightForColumn(measure, actionWidth));
    }

    private void submit() {
        if (!isInputValid()) {
            return;
        }
        String requestedName = profileNameRef.getValue();
        onSubmit.createProfile(requestedName.trim(), Boolean.TRUE.equals(copyDefaultProfileSettingsRef.getValue()));
    }

    private boolean isInputValid() {
        return validationMessage.isEmpty();
    }

    private void refreshValidationState() {
        validationMessage = resolveValidationMessage(profileNameRef.getValue());
        validationLabel.setText(validationMessage);
    }

    @FunctionalInterface
    public interface SubmitProfileCallback {
        void createProfile(String profileName, boolean copyDefaultProfileSettings);
    }
}
