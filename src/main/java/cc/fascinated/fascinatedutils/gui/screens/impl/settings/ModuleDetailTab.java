package cc.fascinated.fascinatedutils.gui.screens.impl.settings;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategory;
import cc.fascinated.fascinatedutils.gui.components.BackButton;
import cc.fascinated.fascinatedutils.gui.components.InputHost;
import cc.fascinated.fascinatedutils.gui.components.ScrollHost;
import cc.fascinated.fascinatedutils.gui.components.UiTranslations;
import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.layout.SpacerNode;
import cc.fascinated.fascinatedutils.gui.core.state.UiState;
import cc.fascinated.fascinatedutils.gui.node.control.ButtonNode;
import cc.fascinated.fascinatedutils.gui.node.popup.ConfirmPopupNode;
import cc.fascinated.fascinatedutils.gui.node.layout.ScrollColumnNode;
import cc.fascinated.fascinatedutils.gui.node.control.SwitchNode;
import cc.fascinated.fascinatedutils.gui.node.display.TextNode;
import cc.fascinated.fascinatedutils.gui.node.input.TextInputNode;
import cc.fascinated.fascinatedutils.gui.node.input.TextParser;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The detail page of one module: its enable switch, a settings search and the module's setting rows.
 */
@RequiredArgsConstructor
public class ModuleDetailTab implements SettingsTab {

    public static final String SEARCH_NODE_ID = "detail.search";

    private static final int SEARCH_WIDTH = 150;

    private final SettingsTabContext context;
    private final Module module;

    @Override
    public void buildHeader(PositionedNode<?> header) {
        SettingsState state = context.getState();
        UiState<String> search = state.detailSearch();
        TextInputNode<String> searchInput = new TextInputNode<>(TextParser.STRING)
                .setPlaceholder(UiTranslations.INSTANCE.get("alumite.setting.shell.search_settings"))
                .setOnChange(search::set)
                .setValue(search.get());
        searchInput.setNodeId(SEARCH_NODE_ID);
        header.addChild(new BackButton(() -> state.openModule().set(null))
                .size(SettingsLayout.CONTROL_HEIGHT, SettingsLayout.CONTROL_HEIGHT));
        header.addChild(new TextNode(module::getDisplayName).setBold(true).alignY(0.5f));
        SwitchNode enabledSwitch = new SwitchNode().setChecked(module.isEnabled());
        enabledSwitch.alignY(0.5f);
        enabledSwitch.setOnChange(checked -> {
            ModuleRegistry.INSTANCE.setModuleEnabled(module, checked);
            module.markUsed();
            ModConfig.profiles().saveModule(module);
        });
        enabledSwitch.setTooltip(() -> context.restrictionReason(module));
        header.addChild(enabledSwitch);
        header.addChild(new SpacerNode());
        header.addChild(new InputHost(searchInput, SEARCH_WIDTH));
        header.addChild(new ButtonNode()
                .setIconCenter(ModUiTextures.RESET.getId())
                .setVariant(ButtonNode.ButtonVariant.GHOST)
                .size(SettingsLayout.CONTROL_HEIGHT, SettingsLayout.CONTROL_HEIGHT)
                .setOnPress(() -> state.moduleReset().set(true))
                .setTooltip(() -> UiTranslations.INSTANCE.get("alumite.setting.ui.reset.tooltip")));
    }

    @Override
    public void buildContents(PositionedNode<?> contents) {
        String searchLower = context.getState().detailSearch().get().toLowerCase(Locale.ROOT);
        boolean filtering = !searchLower.isBlank() && !module.getDisplayName().toLowerCase(Locale.ROOT).contains(searchLower);
        List<Setting<?>> topLevel = module.getSettings().stream()
                .filter(setting -> !filtering || setting.matchesSearch(searchLower))
                .toList();
        List<SettingCategory> categories = new ArrayList<>();
        for (SettingCategory category : module.getSettingCategories()) {
            boolean categoryMatches = filtering && category.nameMatchesSearch(searchLower);
            List<Setting<?>> matching = category.settings().stream()
                    .filter(setting -> !filtering || categoryMatches || setting.matchesSearch(searchLower))
                    .toList();
            categories.add(new SettingCategory(category.displayNameKey(), matching));
        }

        Runnable save = () -> ModConfig.profiles().saveModule(module);
        SettingSectionsBuilder sections = new SettingSectionsBuilder(save, context.getRefresh(),
                setting -> context.getColorPicker().open(setting.getValue(), newColor -> {
                    setting.setValue(newColor);
                    save.run();
                }));
        PositionedNode<?> sectionsNode = sections.build(topLevel, categories);

        ScrollColumnNode list = new ScrollColumnNode().setGap(SettingsLayout.GAP).persistScroll("module.detail." + module.getModuleKey());
        list.addChild(sectionsNode != null ? sectionsNode : new TextNode(() -> UiTranslations.INSTANCE.get(module.getAllSettings().isEmpty()
                ? "alumite.setting.shell.no_settings" : "alumite.setting.shell.no_matching_settings")).setColorResolver(UiTheme::textMuted));
        contents.addChild(new ScrollHost(list, context.getLayout().getContentHeight()));
    }

    @Override
    public void addPopups(PositionedNode<?> root) {
        UiState<Boolean> resetOpen = context.getState().moduleReset();
        if (!resetOpen.get()) {
            return;
        }
        root.addChild(new ConfirmPopupNode()
                .setTitle(UiTranslations.INSTANCE.get("alumite.setting.shell.module_reset_popup_title"))
                .setDescription(Component.translatable("alumite.setting.shell.module_reset_popup_message", module.getDisplayName()).getString())
                .setConfirmLabel(UiTranslations.INSTANCE.get("alumite.setting.shell.module_reset_popup_confirm"))
                .setConfirmLabelColorResolver(UiTheme::danger)
                .setOnCancel(() -> resetOpen.set(false))
                .setOnConfirm(() -> {
                    resetOpen.set(false);
                    module.resetToDefault();
                    ModConfig.profiles().saveModule(module);
                    context.getRefresh().run();
                }));
    }
}
