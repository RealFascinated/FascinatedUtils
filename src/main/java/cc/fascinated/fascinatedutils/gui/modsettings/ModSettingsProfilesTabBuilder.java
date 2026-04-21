package cc.fascinated.fascinatedutils.gui.modsettings;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FCellConstraints;
import cc.fascinated.fascinatedutils.gui.widgets.FColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FLabelWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FMinWidthHostWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FRowWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FScrollColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FSpacerWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import cc.fascinated.fascinatedutils.gui.widgets.SelectableButtonWidget;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.config.impl.profiles.Profile;
import net.minecraft.network.chat.Component;

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
            if (ModConfig.profiles().profileNameExists(requestedName)) {
                return;
            }
            ModConfig.profiles().createProfile(requestedName, false);
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

        FButtonWidget createProfileButton = new SelectableButtonWidget(onOpenCreateProfilePopup, () -> Component.translatable("fascinatedutils.setting.shell.new_profile_button").getString(), settingsInnerWidth, 1, 1f, 6f, 1.12f, 6f, 2f, () -> false) {
            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return controlsHeight;
            }
        };
        createProfileButton.setCellConstraints(new FCellConstraints().setExpandVertical(true).setMinHeight(controlsHeight).setMaxHeight(controlsHeight));
        FWidget topCreateButtonRow = wrapWithSidePad(settingsContentWidth, settingsInnerWidth, createProfileButton);

        List<Profile> profileEntries = new java.util.ArrayList<>(ModConfig.profiles().listProfiles());
        profileEntries.sort((a, b) -> {
            boolean aIsDefault = ModConfig.profiles().isDefaultProfile(a.getProfileId());
            boolean bIsDefault = ModConfig.profiles().isDefaultProfile(b.getProfileId());
            return Boolean.compare(bIsDefault, aIsDefault);
        });
        Optional<UUID> activeProfileId = ModConfig.profiles().getActiveProfileId();
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
            boolean isDefaultProfile = ModConfig.profiles().isDefaultProfile(profileId);

            FButtonWidget profileActionButton = new SelectableButtonWidget(() -> {
                if (isActiveProfile) {
                    return;
                }
                if (ModConfig.profiles().switchActiveProfile(profileId)) {
                    onProfilesChanged.run();
                }
            }, () -> profileEntry.getProfileName(), settingsInnerWidth, 1, 1f, 6f, 1.12f, 6f, 2f, () -> isActiveProfile) {
                @Override
                public boolean mouseDown(float pointerX, float pointerY, int button) {
                    if (button == 1 && !isDefaultProfile) {
                        onProfileAction.onAction(profileId, pointerX, pointerY);
                        return true;
                    }
                    return super.mouseDown(pointerX, pointerY, button);
                }
            };

            scrollBody.addChild(wrapWithSidePad(settingsContentWidth, settingsInnerWidth, profileActionButton));
        }

        FWidget profilesScrollClip = wrapScrollClip(scrollBody, gap, scrollYRef);
        FWidget bottomHudButtonRow = null;
        if (onOpenHudLayoutEditor != null) {
            FButtonWidget editHudLayoutButton = new SelectableButtonWidget(onOpenHudLayoutEditor, () -> Component.translatable("fascinatedutils.setting.shell.edit_hud_layout").getString(), settingsInnerWidth, 1, 1f, 6f, 1.12f, 6f, 2f, () -> false);
            bottomHudButtonRow = wrapWithSidePad(settingsContentWidth, settingsInnerWidth, editHudLayoutButton);
        }

        return new FProfilesPaneLayoutWidget(topCreateButtonRow, profilesScrollClip, bottomHudButtonRow);
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