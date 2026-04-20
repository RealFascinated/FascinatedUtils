package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.*;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.config.profiles.Profile;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ModSettingsProfilesTabBuilder {
    public static FWidget buildProfilesTab(float paneWidth, float paneHeight, Ref<Float> scrollYRef, Ref<String> newProfileNameRef, ProfileActionCallback onProfileAction, Runnable onProfilesChanged) {
        return buildProfilesTab(paneWidth, paneHeight, scrollYRef, newProfileNameRef, onProfileAction, onProfilesChanged, null);
    }

    public static FWidget buildProfilesTab(float paneWidth, float paneHeight, Ref<Float> scrollYRef, Ref<String> newProfileNameRef, ProfileActionCallback onProfileAction, Runnable onProfilesChanged, Runnable onOpenHudLayoutEditor) {
        return buildProfilesPane(paneWidth, paneHeight, scrollYRef, () -> {
            String requestedName = newProfileNameRef.getValue();
            if (requestedName == null || requestedName.isBlank()) {
                return;
            }
            if (ModConfig.profileNameExists(requestedName)) {
                return;
            }
            ModConfig.createProfile(requestedName, false);
            newProfileNameRef.setValue("");
            onProfilesChanged.run();
        }, onProfileAction, onProfilesChanged, onOpenHudLayoutEditor);
    }

    public static FWidget buildProfilesPane(float paneWidth, float paneHeight, Ref<Float> scrollYRef, Runnable onOpenCreateProfilePopup, ProfileActionCallback onProfileAction, Runnable onProfilesChanged) {
        return buildProfilesPane(paneWidth, paneHeight, scrollYRef, onOpenCreateProfilePopup, onProfileAction, onProfilesChanged, null);
    }

    public static FWidget buildProfilesPane(float paneWidth, float paneHeight, Ref<Float> scrollYRef, Runnable onOpenCreateProfilePopup, ProfileActionCallback onProfileAction, Runnable onProfilesChanged, Runnable onOpenHudLayoutEditor) {
        float settingsContentWidth = Math.max(28f, paneWidth);
        float settingsInnerWidth = Math.max(14f, settingsContentWidth - 2f * ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X);
        float controlsHeight = SettingsUiMetrics.SHELL_CONTROL_HEIGHT_DESIGN;
        float gap = 3f;

        FColumnWidget scrollBody = new FColumnWidget(gap, Align.CENTER);

        FButtonWidget createProfileButton = new FButtonWidget(onOpenCreateProfilePopup, () -> Component.translatable("fascinatedutils.setting.shell.new_profile_button").getString(), settingsInnerWidth, 1, 1f, 6f, 1.12f, 6f, 2f) {
            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return controlsHeight;
            }

            @Override
            protected int resolveButtonFillColorArgb(boolean hovered) {
                return hovered ? FascinatedGuiTheme.INSTANCE.moduleListRowHover() : FascinatedGuiTheme.INSTANCE.moduleListRow();
            }

            @Override
            protected int resolveButtonBorderColorArgb(boolean hovered) {
                return super.resolveButtonBorderColorArgb(hovered);
            }
        };
        createProfileButton.setCellConstraints(new FCellConstraints().setExpandVertical(true).setMinHeight(controlsHeight).setMaxHeight(controlsHeight));
        FWidget topCreateButtonRow = wrapWithSidePad(settingsContentWidth, settingsInnerWidth, createProfileButton);

        List<Profile> profileEntries = ModConfig.listProfiles();
        profileEntries.sort((a, b) -> {
            boolean aIsDefault = ModConfig.isDefaultProfile(a.getProfileId());
            boolean bIsDefault = ModConfig.isDefaultProfile(b.getProfileId());
            return Boolean.compare(bIsDefault, aIsDefault);
        });
        Optional<UUID> activeProfileId = ModConfig.getActiveProfileId();
        if (profileEntries.isEmpty()) {
            FLabelWidget emptyLabel = new FLabelWidget();
            emptyLabel.setText(Component.translatable("fascinatedutils.setting.shell.profiles_empty").getString());
            emptyLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
            emptyLabel.setAlignX(Align.START);
            scrollBody.addChild(wrapWithSidePad(settingsContentWidth, settingsInnerWidth, emptyLabel));
        }

        for (Profile profileEntry : profileEntries) {
            UUID profileId = profileEntry.getProfileId();
            boolean isActiveProfile = activeProfileId.isPresent() && activeProfileId.get().equals(profileId);
            boolean isDefaultProfile = ModConfig.isDefaultProfile(profileId);

            FButtonWidget profileActionButton = new FButtonWidget(() -> {
                if (isActiveProfile) {
                    return;
                }
                if (ModConfig.switchActiveProfile(profileId)) {
                    onProfilesChanged.run();
                }
            }, () -> profileEntry.getProfileName(), settingsInnerWidth, 1, 1f, 6f, 1.12f, 6f, 2f) {
                @Override
                public boolean mouseDown(float pointerX, float pointerY, int button) {
                    if (button == 1 && !isDefaultProfile) {
                        onProfileAction.onAction(profileId, pointerX, pointerY);
                        return true;
                    }
                    return super.mouseDown(pointerX, pointerY, button);
                }

                @Override
                protected int resolveButtonFillColorArgb(boolean hovered) {
                    if (isActiveProfile) {
                        return hovered ? FascinatedGuiTheme.INSTANCE.moduleListRowHover() : FascinatedGuiTheme.INSTANCE.moduleListRowSelected();
                    }
                    return hovered ? FascinatedGuiTheme.INSTANCE.moduleListRowHover() : FascinatedGuiTheme.INSTANCE.moduleListRow();
                }

                @Override
                protected int resolveButtonBorderColorArgb(boolean hovered) {
                    if (isActiveProfile) {
                        return hovered ? FascinatedGuiTheme.INSTANCE.borderHover() : FascinatedGuiTheme.INSTANCE.borderMuted();
                    }
                    return super.resolveButtonBorderColorArgb(hovered);
                }
            };

            scrollBody.addChild(wrapWithSidePad(settingsContentWidth, settingsInnerWidth, profileActionButton));
        }

        FWidget profilesScrollClip = wrapScrollClip(scrollBody, gap, scrollYRef);
        FWidget bottomHudButtonRow = null;
        if (onOpenHudLayoutEditor != null) {
            FButtonWidget editHudLayoutButton = new FButtonWidget(onOpenHudLayoutEditor, () -> Component.translatable("fascinatedutils.setting.shell.edit_hud_layout").getString(), settingsInnerWidth, 1, 1f, 6f, 1.12f, 6f, 2f) {
                @Override
                protected int resolveButtonFillColorArgb(boolean hovered) {
                    return hovered ? FascinatedGuiTheme.INSTANCE.moduleListRowHover() : FascinatedGuiTheme.INSTANCE.moduleListRow();
                }

                @Override
                protected int resolveButtonBorderColorArgb(boolean hovered) {
                    return super.resolveButtonBorderColorArgb(hovered);
                }
            };
            bottomHudButtonRow = wrapWithSidePad(settingsContentWidth, settingsInnerWidth, editHudLayoutButton);
        }

        final FWidget finalBottomHudButtonRow = bottomHudButtonRow;
        FWidget pane = new FWidget() {
            {
                addChild(topCreateButtonRow);
                addChild(profilesScrollClip);
                if (finalBottomHudButtonRow != null) {
                    addChild(finalBottomHudButtonRow);
                }
            }

            @Override
            public boolean fillsVerticalInColumn() {
                return true;
            }

            @Override
            public boolean fillsHorizontalInRow() {
                return true;
            }

            @Override
            public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
                setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
                float topInset = 4f;
                float bottomInset = ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X;
                float sectionGap = 3f;
                float createButtonHeight = topCreateButtonRow.intrinsicHeightForColumn(measure, layoutWidth);
                topCreateButtonRow.layout(measure, layoutX, layoutY + topInset, layoutWidth, createButtonHeight);
                float reservedTopHeight = topInset + createButtonHeight + sectionGap;
                float reservedBottomHeight = 0f;
                if (finalBottomHudButtonRow != null) {
                    float buttonHeight = finalBottomHudButtonRow.intrinsicHeightForColumn(measure, layoutWidth);
                    float buttonY = layoutY + Math.max(0f, layoutHeight - bottomInset - buttonHeight);
                    finalBottomHudButtonRow.layout(measure, layoutX, buttonY, layoutWidth, buttonHeight);
                    reservedBottomHeight = buttonHeight + bottomInset + sectionGap;
                }
                float scrollPad = 2f;
                float scrollHeight = Math.max(0f, layoutHeight - reservedTopHeight - reservedBottomHeight - 2f * scrollPad);
                profilesScrollClip.layout(measure, layoutX, layoutY + reservedTopHeight + scrollPad, layoutWidth, scrollHeight);
            }
        };
        return pane;
    }

    private static FWidget wrapWithSidePad(float contentWidth, float innerWidth, FWidget inner) {
        float pad = ModSettingsTheme.SIDEBAR_SEPARATOR_PAD_X;
        FRowWidget row = new FRowWidget(0f, Align.START) {
            @Override
            public boolean fillsHorizontalInRow() {
                return true;
            }
        };
        row.addChild(new FSpacerWidget(pad, 0f));
        FMinWidthHostWidget innerHost = new FMinWidthHostWidget(innerWidth, inner);
        innerHost.setCellConstraints(new FCellConstraints().setExpandHorizontal(true));
        row.addChild(innerHost);
        row.addChild(new FSpacerWidget(pad, 0f));
        return new FMinWidthHostWidget(contentWidth, row);
    }

    private static FWidget wrapScrollClip(FColumnWidget body, float gap, Ref<Float> scrollYRef) {
        FScrollColumnWidget clip = FTheme.components().createScrollColumn(body, gap);
        clip.setFillVerticalInColumn(true);
        if (scrollYRef != null) {
            Float scrollOffsetY = scrollYRef.getValue();
            clip.setScrollOffsetY(scrollOffsetY == null ? 0f : scrollOffsetY);
            clip.setScrollOffsetChangeListener(scrollYRef::setValue);
        }
        return clip;
    }

    @FunctionalInterface
    public interface ProfileActionCallback {
        void onAction(UUID profileId, float contextMenuX, float contextMenuY);
    }
}