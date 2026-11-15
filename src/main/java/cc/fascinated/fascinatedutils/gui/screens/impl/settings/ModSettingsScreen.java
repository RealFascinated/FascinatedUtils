package cc.fascinated.fascinatedutils.gui.screens.impl.settings;

import cc.fascinated.fascinatedutils.client.ModBranding;
import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.common.setting.SettingCategory;
import cc.fascinated.fascinatedutils.common.setting.SettingCategoryGrouper;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.gui.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.SpacerNode;
import cc.fascinated.fascinatedutils.gui.core.UIScale;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.core.UiState;
import cc.fascinated.fascinatedutils.gui.node.ButtonNode;
import cc.fascinated.fascinatedutils.gui.node.CardNode;
import cc.fascinated.fascinatedutils.gui.node.TextureNode;
import cc.fascinated.fascinatedutils.gui.node.RectNode;
import cc.fascinated.fascinatedutils.gui.node.SwitchNode;
import cc.fascinated.fascinatedutils.gui.node.ColorPickerPopupNode;
import cc.fascinated.fascinatedutils.gui.node.ConfirmPopupNode;
import cc.fascinated.fascinatedutils.gui.node.ScrollColumnNode;
import cc.fascinated.fascinatedutils.gui.node.TextNode;
import cc.fascinated.fascinatedutils.gui.node.TextPromptPopupNode;
import cc.fascinated.fascinatedutils.gui.node.input.TextInputNode;
import cc.fascinated.fascinatedutils.gui.node.input.TextParser;
import cc.fascinated.fascinatedutils.gui.render.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.screens.RootScreen;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.settings.Settings;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.config.impl.profiles.Profile;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleIcons;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Mod settings shell built from {@code gui2} nodes: a module grid with per-module detail pages, a profile manager
 * and the global settings registry.
 */
public class ModSettingsScreen extends RootScreen {

    public static final String TAB_MODULES = "modules";
    public static final String TAB_PROFILES = "profiles";
    public static final String TAB_SETTINGS = "settings";

    private static final int MAX_WIDTH = 940;
    private static final int MAX_HEIGHT = 580;
    private static final int MIN_WIDTH = 560;
    private static final int MIN_HEIGHT = 320;
    private static final int SCREEN_MARGIN = 20;
    private static final int WINDOW_PADDING = UiMetrics.SPACE_SM;
    private static final int SIDEBAR_WIDTH = 150;
    private static final int SIDEBAR_PADDING = UiMetrics.SPACE_SM;
    private static final int GAP = UiMetrics.SPACE_SM;
    private static final int CONTROL_HEIGHT = UiMetrics.CONTROL_HEIGHT;
    private static final int SEARCH_WIDTH = 190;
    private static final int ENABLED_COUNT_MIN_MAIN_WIDTH = 560;
    private static final int DETAIL_SEARCH_WIDTH = 150;
    private static final int MIN_TILE_WIDTH = 150;
    private static final int WIDE_GRID_THRESHOLD = 4;
    private static final int TILE_HEIGHT = 132;
    private static final int TILE_ICON_SIZE = 40;
    private static final int TILE_ICON_TOP = 10;
    private static final int TILE_NAME_HEIGHT = 12;
    private static final int TILE_NAME_GAP = 8;
    private static final int SCROLLBAR_RESERVE = 12;
    private static final int EMPTY_STATE_TOP = 64;
    private static final int WORLD_SCRIM = 0x4D080710;

    private static final String MODULE_SEARCH_ID = "modules.search";
    private static final String DETAIL_SEARCH_ID = "detail.search";

    private static String lastTabKey = TAB_MODULES;

    /**
     * Chooses the tab the next settings screen opens on.
     *
     * @param tabKey one of {@link #TAB_MODULES}, {@link #TAB_PROFILES} or {@link #TAB_SETTINGS}
     */
    public static void rememberTab(String tabKey) {
        lastTabKey = tabKey;
    }

    private int windowWidth;
    private int windowHeight;
    private int mainWidth;
    private int contentHeight;
    private int gridColumns;
    private int tileWidth;

    private final @Nullable Module navigateToModule;
    private final @Nullable Screen returnToScreen;

    private TextInputNode<String> moduleSearchInput;
    private TextInputNode<String> detailSearchInput;
    private TextInputNode<String> profileNameInput;
    private ColorPickerPopupNode colorPickerPopup;

    /**
     * Creates the settings screen.
     *
     * @param navigateToModule module whose detail page should open first, or {@code null} for the module grid
     * @param returnToScreen   screen to restore when closing (the HUD editor), or {@code null} to return to the game
     */
    public ModSettingsScreen(@Nullable Module navigateToModule, @Nullable Screen returnToScreen) {
        super(ModBranding.modSettingsScreenTitle());
        this.navigateToModule = navigateToModule;
        this.returnToScreen = returnToScreen;
    }

    @Override
    protected UiNode createRootNode() {
        UiState<String> tab = stateStore.state("tab", navigateToModule != null ? TAB_MODULES : lastTabKey);
        UiState<Module> openModule = stateStore.state("module", navigateToModule);
        UiState<Integer> revision = stateStore.state("revision", 0);
        UiState<ColorSetting> colorTarget = stateStore.state("color.target", (ColorSetting) null);
        Runnable refresh = () -> revision.set(revision.get() + 1);
        computeLayout();

        PositionedNode<?> root = new PositionedNode<>().full();
        root.addChild(new RectNode().setFillArgb(WORLD_SCRIM).full());

        CardNode window = new CardNode().size(windowWidth, windowHeight);
        window.setFillResolver(UiTheme::panelFill);
        window.setBorderResolver(UiTheme::borderSubtle);
        window.setContents(body -> {
            body.margin(WINDOW_PADDING);
            body.rowGap(WINDOW_PADDING);
            body.addChild(buildSidebar(tab, openModule));
            PositionedNode<?> main = new PositionedNode<>().width(mainWidth).height(windowHeight - 2 * WINDOW_PADDING);
            main.columnGap(GAP);
            main.addChild(buildHeader(tab, openModule, refresh));
            PositionedNode<?> contents = new PositionedNode<>().fullWidth().height(contentHeight);
            contents.columnGap(GAP);
            main.addChild(contents);
            body.addChild(main);
            switch (tab.get()) {
                case TAB_PROFILES -> buildProfilesTab(contents, refresh);
                case TAB_SETTINGS -> buildSettingsTab(contents, refresh, colorTarget);
                default -> {
                    Module module = openModule.get();
                    if (module != null) {
                        buildModuleDetail(contents, module, refresh, colorTarget);
                    } else {
                        buildModuleGrid(contents, openModule);
                    }
                }
            }
        });
        window.center();
        root.addChild(window);
        addProfilePopups(root, refresh);
        if (colorTarget.get() != null && colorPickerPopup != null) {
            root.addChild(colorPickerPopup);
        }
        return root;
    }

    @Override
    protected void onEscapePressed() {
        UiState<ColorSetting> colorTarget = stateStore.state("color.target", (ColorSetting) null);
        UiState<Boolean> createOpen = stateStore.state("profile.create", false);
        UiState<UUID> renameId = stateStore.state("profile.rename", (UUID) null);
        UiState<UUID> deleteId = stateStore.state("profile.delete", (UUID) null);
        if (colorTarget.get() != null) {
            colorTarget.set(null);
        } else if (createOpen.get() || renameId.get() != null || deleteId.get() != null) {
            createOpen.set(false);
            renameId.set(null);
            deleteId.set(null);
        } else {
            closeShell();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_F && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            UiState<String> tab = stateStore.state("tab", lastTabKey);
            if (tab.get().equals(TAB_MODULES)) {
                UiState<Integer> revision = stateStore.state("revision", 0);
                stateStore.requestFocus(stateStore.state("module", navigateToModule).get() == null ? MODULE_SEARCH_ID : DETAIL_SEARCH_ID);
                revision.set(revision.get() + 1);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    private void computeLayout() {
        windowWidth = (int) Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, UIScale.screenWidth() - 2 * SCREEN_MARGIN));
        windowHeight = (int) Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, UIScale.screenHeight() - 2 * SCREEN_MARGIN));
        mainWidth = windowWidth - 3 * WINDOW_PADDING - SIDEBAR_WIDTH;
        contentHeight = windowHeight - 2 * WINDOW_PADDING - CONTROL_HEIGHT - GAP;
        int baseColumns = Math.max(2, (mainWidth - SCROLLBAR_RESERVE + GAP) / (MIN_TILE_WIDTH + GAP));
        gridColumns = baseColumns >= WIDE_GRID_THRESHOLD ? baseColumns + 1 : baseColumns;
        tileWidth = (mainWidth - SCROLLBAR_RESERVE - (gridColumns - 1) * GAP) / gridColumns;
    }

    private PositionedNode<?> buildSidebar(UiState<String> tab, UiState<Module> openModule) {
        CardNode sidebar = new CardNode().width(SIDEBAR_WIDTH).fullHeight();
        sidebar.setFillResolver(UiTheme::inputFill);
        sidebar.setNoBorder();
        sidebar.setContents(rail -> {
            rail.left(SIDEBAR_PADDING).right(SIDEBAR_PADDING).top(0).bottom(SIDEBAR_PADDING);
            rail.columnGap(UiMetrics.SPACE_XS);
            rail.addChild(new TextNode(() -> getTitle().getString())
                    .setBold(true)
                    .setTextAlign(0f, 0.5f)
                    .fullWidth()
                    .height(CONTROL_HEIGHT));
            // Zero-height row adds one more gap so the first nav item lines up with the content below the header.
            rail.addChild(new PositionedNode<>().fullWidth().height(0));
            rail.addChild(navButton("alumite.setting.shell.tab_modules", TAB_MODULES, ModUiTextures.SUNNY.getId(), tab, openModule));
            rail.addChild(navButton("alumite.setting.shell.tab_profiles", TAB_PROFILES, ModUiTextures.EDIT.getId(), tab, openModule));
            rail.addChild(navButton("alumite.setting.shell.tab_settings", TAB_SETTINGS, ModUiTextures.SETTINGS.getId(), tab, openModule));
        });
        ButtonNode hudEditor = new ButtonNode(() -> Component.translatable("alumite.setting.shell.edit_hud_layout").getString())
                .setVariant(ButtonNode.ButtonVariant.DEFAULT)
                .setOnPress(this::openHudLayoutEditor);
        hudEditor.left(SIDEBAR_PADDING).right(SIDEBAR_PADDING).bottom(SIDEBAR_PADDING).height(CONTROL_HEIGHT);
        sidebar.addChild(hudEditor);
        return sidebar;
    }

    private ButtonNode navButton(String translationKey, String tabKey, Identifier icon, UiState<String> tab, UiState<Module> openModule) {
        ButtonNode button = new ButtonNode(() -> Component.translatable(translationKey).getString())
                .setLeftIcon(icon)
                .setVariant(() -> tab.get().equals(tabKey) ? ButtonNode.ButtonVariant.PRIMARY : ButtonNode.ButtonVariant.GHOST)
                .setOnPress(() -> {
                    lastTabKey = tabKey;
                    openModule.set(null);
                    tab.set(tabKey);
                });
        button.fullWidth().height(CONTROL_HEIGHT);
        return button;
    }

    private PositionedNode<?> buildHeader(UiState<String> tab, UiState<Module> openModule, Runnable refresh) {
        PositionedNode<?> header = new PositionedNode<>().fullWidth().height(CONTROL_HEIGHT).rowGap(GAP);
        Module module = openModule.get();
        if (tab.get().equals(TAB_MODULES) && module == null) {
            buildGridHeader(header);
        } else if (tab.get().equals(TAB_MODULES)) {
            buildDetailHeader(header, module, openModule, refresh);
        } else {
            String titleKey = tab.get().equals(TAB_PROFILES) ? "alumite.setting.shell.tab_profiles" : "alumite.setting.shell.tab_settings";
            header.addChild(new TextNode(() -> Component.translatable(titleKey).getString()).setBold(true).alignY(0.5f));
            header.addChild(new SpacerNode());
        }
        if (tab.get().equals(TAB_PROFILES)) {
            UiState<Boolean> createOpen = stateStore.state("profile.create", false);
            UiState<String> profileName = stateStore.state("profile.name", "");
            header.addChild(new ButtonNode(() -> Component.translatable("alumite.setting.shell.new_profile_button").getString())
                    .setLeftIcon(ModUiTextures.ADD.getId())
                    .setVariant(ButtonNode.ButtonVariant.PRIMARY)
                    .height(CONTROL_HEIGHT)
                    .setOnPress(() -> {
                        profileName.set("");
                        prepareProfileInput(profileName, "");
                        createOpen.set(true);
                    }));
        }
        header.addChild(new ButtonNode()
                .setIconCenter(ModUiTextures.CLOSE.getId())
                .setVariant(ButtonNode.ButtonVariant.GHOST)
                .size(CONTROL_HEIGHT, CONTROL_HEIGHT)
                .setOnPress(this::closeShell));
        return header;
    }

    private void buildGridHeader(PositionedNode<?> header) {
        UiState<String> search = stateStore.state("modules.search", "");
        UiState<ModuleCategory> filter = stateStore.state("modules.filter", (ModuleCategory) null);
        if (moduleSearchInput == null) {
            moduleSearchInput = new TextInputNode<>(TextParser.STRING)
                    .setPlaceholder(Component.translatable("alumite.setting.shell.search_modules").getString())
                    .setOnChange(search::set)
                    .setValue(search.get());
            moduleSearchInput.setNodeId(MODULE_SEARCH_ID);
        }
        header.addChild(inputHost(moduleSearchInput, SEARCH_WIDTH));
        PositionedNode<?> chips = new PositionedNode<>().height(CONTROL_HEIGHT).rowGap(UiMetrics.SPACE_XS);
        chips.addChild(filterButton(() -> Component.translatable("alumite.setting.shell.filter_all").getString(), null, filter));
        for (ModuleCategory category : ModuleCategory.values()) {
            chips.addChild(filterButton(category::getDisplayName, category, filter));
        }
        header.addChild(chips);
        header.addChild(new SpacerNode());
        if (mainWidth < ENABLED_COUNT_MIN_MAIN_WIDTH) {
            return;
        }
        List<Module> allModules = ModuleRegistry.INSTANCE.getModules();
        header.addChild(new TextNode(() -> Component.translatable("alumite.setting.shell.enabled_count",
                allModules.stream().filter(Module::isEnabled).count(), allModules.size()).getString())
                .setColorResolver(UiTheme::textSubtle)
                .alignY(0.5f));
    }

    private void buildDetailHeader(PositionedNode<?> header, Module module, UiState<Module> openModule, Runnable refresh) {
        UiState<String> search = stateStore.state("detail.search", "");
        if (detailSearchInput == null) {
            detailSearchInput = new TextInputNode<>(TextParser.STRING)
                    .setPlaceholder(Component.translatable("alumite.setting.shell.search_settings").getString())
                    .setOnChange(search::set)
                    .setValue(search.get());
            detailSearchInput.setNodeId(DETAIL_SEARCH_ID);
        }
        header.addChild(new ButtonNode()
                .setIconCenter(ModUiTextures.BACK.getId())
                .setVariant(ButtonNode.ButtonVariant.GHOST)
                .size(CONTROL_HEIGHT, CONTROL_HEIGHT)
                .setOnPress(() -> openModule.set(null)));
        header.addChild(new TextNode(module::getDisplayName).setBold(true).alignY(0.5f));
        SwitchNode enabledSwitch = new SwitchNode().setChecked(module.isEnabled());
        enabledSwitch.alignY(0.5f);
        enabledSwitch.setOnChange(checked -> {
            ModuleRegistry.INSTANCE.setModuleEnabled(module, checked);
            ModConfig.profiles().saveModule(module);
        });
        header.addChild(enabledSwitch);
        header.addChild(new SpacerNode());
        header.addChild(inputHost(detailSearchInput, DETAIL_SEARCH_WIDTH));
        header.addChild(new ButtonNode()
                .setIconCenter(ModUiTextures.RESET.getId())
                .setVariant(ButtonNode.ButtonVariant.GHOST)
                .size(CONTROL_HEIGHT, CONTROL_HEIGHT)
                .setOnPress(() -> {
                    module.resetToDefault();
                    ModConfig.profiles().saveModule(module);
                    refresh.run();
                }));
    }

    private void buildModuleGrid(PositionedNode<?> contents, UiState<Module> openModule) {
        UiState<String> search = stateStore.state("modules.search", "");
        UiState<ModuleCategory> filter = stateStore.state("modules.filter", (ModuleCategory) null);

        String searchLower = search.get().toLowerCase(Locale.ROOT);
        List<Module> modules = new ArrayList<>(ModuleRegistry.INSTANCE.getModules().stream()
                .filter(module -> filter.get() == null || module.getCategory() == filter.get())
                .filter(module -> module.getDisplayName().toLowerCase(Locale.ROOT).contains(searchLower))
                .toList());
        modules.sort((left, right) -> String.CASE_INSENSITIVE_ORDER.compare(left.getDisplayName(), right.getDisplayName()));

        ScrollColumnNode list = new ScrollColumnNode().setGap(GAP).persistScroll("modules.grid");
        if (modules.isEmpty()) {
            list.addChild(noResultsState(search, filter));
        }
        for (int index = 0; index < modules.size(); index += gridColumns) {
            PositionedNode<?> row = new PositionedNode<>().fullWidth().height(TILE_HEIGHT).rowGap(GAP);
            for (int column = index; column < Math.min(index + gridColumns, modules.size()); column++) {
                row.addChild(moduleTile(modules.get(column), openModule));
            }
            list.addChild(row);
        }
        contents.addChild(scrollHost(list, contentHeight));
    }

    private PositionedNode<?> noResultsState(UiState<String> search, UiState<ModuleCategory> filter) {
        PositionedNode<?> state = new PositionedNode<>().fullWidth().columnGap(UiMetrics.SPACE_SM);
        state.addChild(new PositionedNode<>().fullWidth().height(EMPTY_STATE_TOP));
        state.addChild(new TextNode(() -> Component.translatable("alumite.setting.shell.no_results_title").getString())
                .setBold(true)
                .alignX(0.5f));
        state.addChild(new TextNode(() -> Component.translatable("alumite.setting.shell.no_results_hint").getString())
                .setColorResolver(UiTheme::textMuted)
                .alignX(0.5f));
        state.addChild(new ButtonNode(() -> Component.translatable("alumite.setting.shell.clear_filters").getString())
                .alignX(0.5f)
                .setOnPress(() -> {
                    moduleSearchInput.setValue("");
                    search.set("");
                    filter.set(null);
                }));
        return state;
    }

    private ButtonNode filterButton(Supplier<String> label, @Nullable ModuleCategory category, UiState<ModuleCategory> filter) {
        return new ButtonNode(label)
                .setVariant(() -> filter.get() == category ? ButtonNode.ButtonVariant.DEFAULT : ButtonNode.ButtonVariant.GHOST)
                .setLabelColorResolver(theme -> filter.get() == category ? theme.textPrimary() : theme.textMuted())
                .setOnPress(() -> filter.set(category));
    }

    private CardNode moduleTile(Module module, UiState<Module> openModule) {
        CardNode tile = new CardNode().width(tileWidth).height(TILE_HEIGHT);
        tile.setFillResolver(UiTheme::surfaceCard);
        tile.setBorderResolver(UiTheme::borderSubtle);
        tile.setContents(content -> {
            content.addChild(new TextureNode()
                    .setTexture(ModuleIcons.INSTANCE.iconFor(module))
                    .setTintResolver(theme -> module.isEnabled() ? theme.textPrimary() : theme.textSubtle())
                    .size(TILE_ICON_SIZE).alignX(0.5f).top(TILE_ICON_TOP));
            content.addChild(new TextNode(module::getDisplayName)
                    .setBold(true)
                    .setColorResolver(theme -> module.isEnabled() ? theme.textPrimary() : theme.textMuted())
                    .setTextAlign(0.5f, 0.5f)
                    .fullWidth().height(TILE_NAME_HEIGHT)
                    .top(TILE_ICON_TOP + TILE_ICON_SIZE + TILE_NAME_GAP));

            ButtonNode options = new ButtonNode(() -> Component.translatable("alumite.setting.shell.module_options").getString().toUpperCase(Locale.ROOT))
                    .setCornerMask(RectCornerRoundMask.TOP)
                    .setOnPress(() -> openModuleDetail(module, openModule));
            options.size(tileWidth, CONTROL_HEIGHT).left(0).bottom(CONTROL_HEIGHT);
            content.addChild(options);

            // Enabled is a solid green bar, disabled a solid red one.
            ButtonNode status = new ButtonNode(() -> Component.translatable(module.isEnabled()
                    ? "alumite.setting.shell.widget_state.enabled" : "alumite.setting.shell.widget_state.disabled").getString().toUpperCase(Locale.ROOT))
                    .setCornerMask(RectCornerRoundMask.BOTTOM)
                    .setVariant(() -> module.isEnabled() ? ButtonNode.ButtonVariant.SUCCESS : ButtonNode.ButtonVariant.DANGER)
                    .setOnPress(() -> {
                        ModuleRegistry.INSTANCE.setModuleEnabled(module, !module.isEnabled());
                        ModConfig.profiles().saveModule(module);
                    });
            status.size(tileWidth, CONTROL_HEIGHT).left(0).bottom(0);
            content.addChild(status);
        });
        return tile;
    }

    private void openModuleDetail(Module module, UiState<Module> openModule) {
        UiState<String> detailSearch = stateStore.state("detail.search", "");
        detailSearch.set("");
        if (detailSearchInput != null) {
            detailSearchInput.setValue("");
        }
        openModule.set(module);
    }

    private void buildModuleDetail(PositionedNode<?> contents, Module module, Runnable refresh, UiState<ColorSetting> colorTarget) {
        UiState<String> search = stateStore.state("detail.search", "");

        String searchLower = search.get().toLowerCase(Locale.ROOT);
        boolean filtering = !searchLower.isBlank();
        List<Setting<?>> topLevel = module.getSettings().stream()
                .filter(setting -> !filtering || setting.getName().toLowerCase(Locale.ROOT).contains(searchLower))
                .toList();
        List<SettingCategory> categories = new ArrayList<>();
        for (SettingCategory category : module.getSettingCategories()) {
            List<Setting<?>> matching = category.settings().stream()
                    .filter(setting -> !filtering || setting.getName().toLowerCase(Locale.ROOT).contains(searchLower))
                    .toList();
            categories.add(new SettingCategory(category.displayNameKey(), matching));
        }

        SettingSectionsBuilder sections = new SettingSectionsBuilder(
                () -> ModConfig.profiles().saveModule(module), refresh, setting -> openColorPicker(setting, colorTarget, () -> ModConfig.profiles().saveModule(module)));
        PositionedNode<?> sectionsNode = sections.build(topLevel, categories);

        ScrollColumnNode list = new ScrollColumnNode().setGap(GAP).persistScroll("module.detail." + module.getDisplayName());
        list.addChild(sectionsNode != null ? sectionsNode : emptyLabel(module.getAllSettings().isEmpty()
                ? "alumite.setting.shell.no_settings" : "alumite.setting.shell.no_matching_settings"));
        contents.addChild(scrollHost(list, contentHeight));
    }

    private void buildSettingsTab(PositionedNode<?> contents, Runnable refresh, UiState<ColorSetting> colorTarget) {
        List<Setting<?>> allSettings = Settings.INSTANCE.getSettings();
        SettingSectionsBuilder sections = new SettingSectionsBuilder(
                () -> ModConfig.config().save(), refresh, setting -> openColorPicker(setting, colorTarget, () -> ModConfig.config().save()));
        PositionedNode<?> sectionsNode = sections.build(
                SettingCategoryGrouper.topLevelInRegistrationOrder(allSettings),
                SettingCategoryGrouper.categoriesInRegistrationOrder(allSettings));

        ScrollColumnNode list = new ScrollColumnNode().setGap(GAP).persistScroll("settings.registry");
        list.addChild(sectionsNode != null ? sectionsNode : emptyLabel("alumite.setting.shell.settings_empty"));
        contents.addChild(scrollHost(list, contentHeight));
    }

    private void buildProfilesTab(PositionedNode<?> contents, Runnable refresh) {
        UiState<Boolean> createOpen = stateStore.state("profile.create", false);
        UiState<UUID> renameId = stateStore.state("profile.rename", (UUID) null);
        UiState<UUID> deleteId = stateStore.state("profile.delete", (UUID) null);
        UiState<String> profileName = stateStore.state("profile.name", "");

        List<Profile> profiles = new ArrayList<>(ModConfig.profiles().listProfiles());
        profiles.sort((left, right) -> Boolean.compare(
                ModConfig.profiles().isDefaultProfile(right.getProfileId()), ModConfig.profiles().isDefaultProfile(left.getProfileId())));
        UUID activeId = ModConfig.profiles().getActiveProfileId().orElse(null);

        ScrollColumnNode list = new ScrollColumnNode().setGap(GAP).persistScroll("profiles.list");
        if (profiles.isEmpty()) {
            list.addChild(emptyLabel("alumite.setting.shell.profiles_empty"));
        }
        for (Profile profile : profiles) {
            UUID profileId = profile.getProfileId();
            boolean active = profileId.equals(activeId);
            boolean isDefault = ModConfig.profiles().isDefaultProfile(profileId);
            CardNode profileCard = new CardNode().height(UiMetrics.LIST_ROW_HEIGHT).fullWidth();
            profileCard.setFillResolver(UiTheme::surfaceCard);
            profileCard.setBorderResolver(theme -> active ? theme.accent() : theme.borderSubtle());
            profileCard.setContents(row -> {
                row.rowGap(GAP);
                row.left(UiMetrics.SPACE_MD).right(UiMetrics.SPACE_SM);
                row.addChild(new TextNode(profile::getProfileName).setBold(true).alignY(0.5f));
                if (active) {
                    row.addChild(new TextNode(() -> Component.translatable("alumite.setting.shell.profile_active").getString())
                            .setColorResolver(UiTheme::success).alignY(0.5f));
                }
                row.addChild(new SpacerNode());
                if (!active) {
                    row.addChild(new ButtonNode(() -> Component.translatable("alumite.setting.shell.profile_use").getString())
                            .height(CONTROL_HEIGHT)
                            .alignY(0.5f)
                            .setOnPress(() -> {
                                if (ModConfig.profiles().switchActiveProfile(profileId)) {
                                    refresh.run();
                                }
                            }));
                }
                if (!isDefault) {
                    row.addChild(new ButtonNode()
                            .setIconCenter(ModUiTextures.EDIT.getId())
                            .size(CONTROL_HEIGHT, CONTROL_HEIGHT)
                            .alignY(0.5f)
                            .setOnPress(() -> {
                                profileName.set(profile.getProfileName());
                                prepareProfileInput(profileName, profile.getProfileName());
                                renameId.set(profileId);
                            }));
                    row.addChild(new ButtonNode()
                            .setIconCenter(ModUiTextures.TRASH.getId())
                            .setVariant(ButtonNode.ButtonVariant.DANGER)
                            .size(CONTROL_HEIGHT, CONTROL_HEIGHT)
                            .alignY(0.5f)
                            .setOnPress(() -> deleteId.set(profileId)));
                }
            });
            list.addChild(profileCard);
        }
        contents.addChild(scrollHost(list, contentHeight));
    }

    private void prepareProfileInput(UiState<String> profileName, String initialValue) {
        if (profileNameInput == null) {
            profileNameInput = new TextInputNode<>(TextParser.STRING)
                    .setPlaceholder(Component.translatable("alumite.setting.shell.profile_name_placeholder").getString())
                    .setOnChange(profileName::set);
            profileNameInput.setNodeId("profile.name");
        }
        profileNameInput.setValue(initialValue);
        stateStore.requestFocus("profile.name");
    }

    private void addProfilePopups(PositionedNode<?> root, Runnable refresh) {
        UiState<Boolean> createOpen = stateStore.state("profile.create", false);
        UiState<UUID> renameId = stateStore.state("profile.rename", (UUID) null);
        UiState<UUID> deleteId = stateStore.state("profile.delete", (UUID) null);
        UiState<String> profileName = stateStore.state("profile.name", "");

        if (createOpen.get() && profileNameInput != null) {
            root.addChild(new TextPromptPopupNode(
                    () -> Component.translatable("alumite.setting.shell.profile_popup_title").getString(),
                    profileNameInput,
                    () -> Component.translatable("alumite.setting.shell.profile_popup_create").getString(),
                    () -> profileName.get().isBlank() || ModConfig.profiles().profileNameExists(profileName.get()))
                    .setOnCancel(() -> createOpen.set(false))
                    .setOnConfirm(() -> {
                        String requestedName = profileName.get().trim();
                        if (requestedName.isEmpty() || ModConfig.profiles().profileNameExists(requestedName)) {
                            return;
                        }
                        ModConfig.profiles().createProfile(requestedName);
                        createOpen.set(false);
                        refresh.run();
                    }));
        }
        UUID renaming = renameId.get();
        if (renaming != null && profileNameInput != null) {
            root.addChild(new TextPromptPopupNode(
                    () -> Component.translatable("alumite.setting.shell.profile_rename_popup_title").getString(),
                    profileNameInput,
                    () -> Component.translatable("alumite.setting.shell.profile_rename_popup_confirm").getString(),
                    () -> profileName.get().isBlank() || ModConfig.profiles().profileNameExists(profileName.get()))
                    .setOnCancel(() -> renameId.set(null))
                    .setOnConfirm(() -> {
                        if (ModConfig.profiles().renameProfile(renaming, profileName.get())) {
                            renameId.set(null);
                            refresh.run();
                        }
                    }));
        }
        UUID deleting = deleteId.get();
        if (deleting != null) {
            String deletingName = ModConfig.profiles().listProfiles().stream()
                    .filter(profile -> profile.getProfileId().equals(deleting))
                    .map(Profile::getProfileName)
                    .findFirst().orElse("");
            root.addChild(new ConfirmPopupNode()
                    .setTitle(Component.translatable("alumite.setting.shell.profile_delete_popup_title").getString())
                    .setDescription(Component.translatable("alumite.setting.shell.profile_delete_popup_message", deletingName).getString())
                    .setConfirmLabel(Component.translatable("alumite.setting.shell.profile_delete_button").getString())
                    .setConfirmLabelColorResolver(UiTheme::danger)
                    .setOnCancel(() -> deleteId.set(null))
                    .setOnConfirm(() -> {
                        ModConfig.profiles().deleteProfile(deleting);
                        deleteId.set(null);
                        refresh.run();
                    }));
        }
    }

    private void openColorPicker(ColorSetting setting, UiState<ColorSetting> colorTarget, Runnable save) {
        colorPickerPopup = new ColorPickerPopupNode(
                setting.getValue().copy(),
                newColor -> {
                    setting.setValue(newColor);
                    save.run();
                    colorTarget.set(null);
                },
                () -> colorTarget.set(null));
        colorTarget.set(setting);
    }

    private PositionedNode<?> inputHost(TextInputNode<String> input, int width) {
        PositionedNode<?> host = new PositionedNode<>().width(width).height(CONTROL_HEIGHT);
        host.addChild(input);
        return host;
    }

    private PositionedNode<?> scrollHost(ScrollColumnNode scroll, int height) {
        PositionedNode<?> host = new PositionedNode<>().fullWidth().height(height);
        host.addChild(scroll);
        return host;
    }

    private TextNode emptyLabel(String translationKey) {
        return new TextNode(() -> Component.translatable(translationKey).getString()).setColorResolver(UiTheme::textMuted);
    }

    private void openHudLayoutEditor() {
        if (returnToScreen != null) {
            HUDManager.INSTANCE.markEditModeActive();
            Minecraft.getInstance().setScreen(returnToScreen);
        } else {
            HUDManager.INSTANCE.setEditMode(true);
        }
    }

    private void closeShell() {
        if (returnToScreen != null) {
            HUDManager.INSTANCE.markEditModeActive();
            Minecraft.getInstance().setScreen(returnToScreen);
        } else {
            Minecraft.getInstance().setScreen(null);
        }
    }
}
