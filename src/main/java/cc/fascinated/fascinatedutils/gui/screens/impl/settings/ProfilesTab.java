package cc.fascinated.fascinatedutils.gui.screens.impl.settings;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.components.ScrollHost;
import cc.fascinated.fascinatedutils.gui.components.SurfaceCard;
import cc.fascinated.fascinatedutils.gui.components.UiTranslations;
import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.layout.SpacerNode;
import cc.fascinated.fascinatedutils.gui.core.state.UiState;
import cc.fascinated.fascinatedutils.gui.node.control.ButtonNode;
import cc.fascinated.fascinatedutils.gui.node.display.CardNode;
import cc.fascinated.fascinatedutils.gui.node.popup.ConfirmPopupNode;
import cc.fascinated.fascinatedutils.gui.node.layout.ScrollColumnNode;
import cc.fascinated.fascinatedutils.gui.node.display.TextNode;
import cc.fascinated.fascinatedutils.gui.node.popup.TextPromptPopupNode;
import cc.fascinated.fascinatedutils.gui.node.input.TextInputNode;
import cc.fascinated.fascinatedutils.gui.node.input.TextParser;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.config.impl.profiles.Profile;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The profile manager page: lists profiles and owns the create, rename and delete popups.
 */
@RequiredArgsConstructor
public class ProfilesTab implements SettingsTab {

    private static final String NAME_NODE_ID = "profile.name";

    private final SettingsTabContext context;

    @Override
    public void buildHeader(PositionedNode<?> header) {
        SettingsState state = context.getState();
        header.addChild(new TextNode(() -> UiTranslations.INSTANCE.get("alumite.setting.shell.tab_profiles")).setBold(true).alignY(0.5f));
        header.addChild(new SpacerNode());
        header.addChild(new ButtonNode(() -> UiTranslations.INSTANCE.get("alumite.setting.shell.new_profile_button"))
                .setLeftIcon(ModUiTextures.ADD.getId())
                .setVariant(ButtonNode.ButtonVariant.PRIMARY)
                .height(SettingsLayout.CONTROL_HEIGHT)
                .setOnPress(() -> openNamePrompt(state.profileCreate()::set, ""))); 
    }

    @Override
    public void buildContents(PositionedNode<?> contents) {
        SettingsState state = context.getState();
        List<Profile> profiles = new ArrayList<>(ModConfig.profiles().listProfiles());
        profiles.sort((left, right) -> Boolean.compare(
                ModConfig.profiles().isDefaultProfile(right.getProfileId()), ModConfig.profiles().isDefaultProfile(left.getProfileId())));
        UUID activeId = ModConfig.profiles().getActiveProfileId().orElse(null);

        ScrollColumnNode list = new ScrollColumnNode().setGap(SettingsLayout.GAP).persistScroll("profiles.list");
        if (profiles.isEmpty()) {
            list.addChild(new TextNode(() -> UiTranslations.INSTANCE.get("alumite.setting.shell.profiles_empty")).setColorResolver(UiTheme::textMuted));
        }
        for (Profile profile : profiles) {
            UUID profileId = profile.getProfileId();
            boolean active = profileId.equals(activeId);
            boolean isDefault = ModConfig.profiles().isDefaultProfile(profileId);
            CardNode profileCard = new SurfaceCard().height(UiMetrics.LIST_ROW_HEIGHT).fullWidth();
            profileCard.setBorderResolver(theme -> active ? theme.accent() : theme.borderSubtle());
            profileCard.setContents(row -> {
                row.rowGap(SettingsLayout.GAP);
                row.left(UiMetrics.SPACE_MD).right(UiMetrics.SPACE_SM);
                row.addChild(new TextNode(profile::getProfileName).setBold(true).alignY(0.5f));
                if (active) {
                    row.addChild(new TextNode(() -> UiTranslations.INSTANCE.get("alumite.setting.shell.profile_active"))
                            .setColorResolver(UiTheme::success).alignY(0.5f));
                }
                row.addChild(new SpacerNode());
                if (!active) {
                    row.addChild(new ButtonNode(() -> UiTranslations.INSTANCE.get("alumite.setting.shell.profile_use"))
                            .height(SettingsLayout.CONTROL_HEIGHT)
                            .alignY(0.5f)
                            .setOnPress(() -> {
                                if (ModConfig.profiles().switchActiveProfile(profileId)) {
                                    context.getRefresh().run();
                                }
                            }));
                }
                if (!isDefault) {
                    row.addChild(new ButtonNode()
                            .setIconCenter(ModUiTextures.EDIT.getId())
                            .size(SettingsLayout.CONTROL_HEIGHT, SettingsLayout.CONTROL_HEIGHT)
                            .alignY(0.5f)
                            .setOnPress(() -> openNamePrompt(open -> state.profileRename().set(open ? profileId : null), profile.getProfileName()))
                            .setTooltip(() -> UiTranslations.INSTANCE.get("alumite.setting.shell.profile_rename_tooltip")));
                    row.addChild(new ButtonNode()
                            .setIconCenter(ModUiTextures.TRASH.getId())
                            .setVariant(ButtonNode.ButtonVariant.DANGER)
                            .size(SettingsLayout.CONTROL_HEIGHT, SettingsLayout.CONTROL_HEIGHT)
                            .alignY(0.5f)
                            .setOnPress(() -> state.profileDelete().set(profileId))
                            .setTooltip(() -> UiTranslations.INSTANCE.get("alumite.setting.shell.profile_delete_button")));
                }
            });
            list.addChild(profileCard);
        }
        contents.addChild(new ScrollHost(list, context.getLayout().getContentHeight()));
    }

    @Override
    public void addPopups(PositionedNode<?> root) {
        SettingsState state = context.getState();
        UiState<String> profileName = state.profileName();
        if (state.profileCreate().get()) {
            root.addChild(new TextPromptPopupNode(
                    () -> UiTranslations.INSTANCE.get("alumite.setting.shell.profile_popup_title"),
                    nameInput(profileName),
                    () -> UiTranslations.INSTANCE.get("alumite.setting.shell.profile_popup_create"),
                    () -> profileName.get().isBlank() || ModConfig.profiles().profileNameExists(profileName.get()))
                    .setErrorSupplier(() -> !profileName.get().isBlank() && ModConfig.profiles().profileNameExists(profileName.get())
                            ? UiTranslations.INSTANCE.get("alumite.setting.shell.profile_name_error_duplicate_create") : null)
                    .setOnCancel(() -> state.profileCreate().set(false))
                    .setOnConfirm(() -> {
                        String requestedName = profileName.get().trim();
                        if (requestedName.isEmpty() || ModConfig.profiles().profileNameExists(requestedName)) {
                            return;
                        }
                        ModConfig.profiles().createProfile(requestedName);
                        state.profileCreate().set(false);
                        context.getRefresh().run();
                    }));
        }
        UUID renaming = state.profileRename().get();
        if (renaming != null) {
            String currentName = profileNameOf(renaming);
            root.addChild(new TextPromptPopupNode(
                    () -> UiTranslations.INSTANCE.get("alumite.setting.shell.profile_rename_popup_title"),
                    nameInput(profileName),
                    () -> UiTranslations.INSTANCE.get("alumite.setting.shell.profile_rename_popup_confirm"),
                    () -> profileName.get().isBlank() || ModConfig.profiles().profileNameExists(profileName.get()))
                    .setErrorSupplier(() -> {
                        if (profileName.get().isBlank()) {
                            return UiTranslations.INSTANCE.get("alumite.setting.shell.profile_name_error_empty");
                        }
                        if (!profileName.get().trim().equalsIgnoreCase(currentName.trim()) && ModConfig.profiles().profileNameExists(profileName.get())) {
                            return UiTranslations.INSTANCE.get("alumite.setting.shell.profile_name_error_duplicate_rename");
                        }
                        return null;
                    })
                    .setOnCancel(() -> state.profileRename().set(null))
                    .setOnConfirm(() -> {
                        if (ModConfig.profiles().renameProfile(renaming, profileName.get())) {
                            state.profileRename().set(null);
                            context.getRefresh().run();
                        }
                    }));
        }
        UUID deleting = state.profileDelete().get();
        if (deleting != null) {
            root.addChild(new ConfirmPopupNode()
                    .setTitle(UiTranslations.INSTANCE.get("alumite.setting.shell.profile_delete_popup_title"))
                    .setDescription(Component.translatable("alumite.setting.shell.profile_delete_popup_message", profileNameOf(deleting)).getString())
                    .setConfirmLabel(UiTranslations.INSTANCE.get("alumite.setting.shell.profile_delete_button"))
                    .setConfirmLabelColorResolver(UiTheme::danger)
                    .setOnCancel(() -> state.profileDelete().set(null))
                    .setOnConfirm(() -> {
                        ModConfig.profiles().deleteProfile(deleting);
                        state.profileDelete().set(null);
                        context.getRefresh().run();
                    }));
        }
    }

    private String profileNameOf(UUID profileId) {
        return ModConfig.profiles().listProfiles().stream()
                .filter(profile -> profile.getProfileId().equals(profileId))
                .map(Profile::getProfileName)
                .findFirst().orElse("");
    }

    private TextInputNode<String> nameInput(UiState<String> profileName) {
        TextInputNode<String> input = new TextInputNode<>(TextParser.STRING)
                .setPlaceholder(UiTranslations.INSTANCE.get("alumite.setting.shell.profile_name_placeholder"))
                .setOnChange(profileName::set)
                .setValue(profileName.get());
        input.setNodeId(NAME_NODE_ID);
        return input;
    }

    private void openNamePrompt(Consumer<Boolean> open, String initialName) {
        context.getState().profileName().set(initialName);
        context.getStateStore().requestFocus(NAME_NODE_ID);
        open.accept(true);
    }
}
