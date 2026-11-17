package cc.fascinated.fascinatedutils.gui.screens.impl.settings;

import cc.fascinated.fascinatedutils.client.ModBranding;
import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.components.CloseButton;
import cc.fascinated.fascinatedutils.gui.components.ColorPickerController;
import cc.fascinated.fascinatedutils.gui.components.PanelCard;
import cc.fascinated.fascinatedutils.gui.components.UiTranslations;
import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.core.state.UiState;
import cc.fascinated.fascinatedutils.gui.node.control.ButtonNode;
import cc.fascinated.fascinatedutils.gui.node.display.CardNode;
import cc.fascinated.fascinatedutils.gui.node.display.RectNode;
import cc.fascinated.fascinatedutils.gui.node.display.TextNode;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui.screens.RootScreen;
import cc.fascinated.fascinatedutils.systems.hud.HudManager;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Mod settings shell: a sidebar of tabs around a module grid with per-module detail pages, a profile manager and the
 * global settings registry. Each page is a {@link SettingsTab}; this screen only owns the window, the sidebar and the
 * header chrome.
 */
public class ModSettingsScreen extends RootScreen {

    public static final String TAB_MODULES = "modules";
    public static final String TAB_PROFILES = "profiles";
    public static final String TAB_SETTINGS = "settings";

    private static final int SIDEBAR_PADDING = UiMetrics.SPACE_SM;
    private static final int WORLD_SCRIM = 0x4D080710;

    /**
     * Tab the next settings screen opens on. Deliberately static: it is session memory that outlives any one screen
     * instance, so reopening the settings returns to the page the player last used.
     */
    private static String lastTabKey = TAB_MODULES;

    /**
     * Chooses the tab the next settings screen opens on.
     *
     * @param tabKey one of {@link #TAB_MODULES}, {@link #TAB_PROFILES} or {@link #TAB_SETTINGS}
     */
    public static void rememberTab(String tabKey) {
        lastTabKey = tabKey;
    }

    private final @Nullable Screen returnToScreen;
    private final SettingsState settingsState;
    private final ColorPickerController colorPicker = new ColorPickerController(stateStore, "color.picker.open");

    /**
     * Creates the settings screen.
     *
     * @param navigateToModule module whose detail page should open first, or {@code null} for the module grid
     * @param returnToScreen   screen to restore when closing (the HUD editor), or {@code null} to return to the game
     */
    public ModSettingsScreen(@Nullable Module navigateToModule, @Nullable Screen returnToScreen) {
        super(ModBranding.modSettingsScreenTitle());
        this.returnToScreen = returnToScreen;
        this.settingsState = new SettingsState(stateStore, navigateToModule, navigateToModule != null ? TAB_MODULES : lastTabKey);
    }

    @Override
    protected UiNode composeContent() {
        UiState<Integer> revision = settingsState.revision();
        Runnable refresh = () -> revision.set(revision.get() + 1);
        SettingsLayout layout = new SettingsLayout();
        SettingsTabContext context = new SettingsTabContext(stateStore, settingsState, layout, refresh, colorPicker);
        SettingsTab tab = currentTab(context);

        PositionedNode<?> root = new PositionedNode<>().full();
        root.addChild(new RectNode().setFillArgb(WORLD_SCRIM).full());

        CardNode window = new PanelCard().size(layout.getWindowWidth(), layout.getWindowHeight());
        window.setContents(body -> {
            body.margin(SettingsLayout.WINDOW_PADDING);
            body.rowGap(SettingsLayout.WINDOW_PADDING);
            body.addChild(buildSidebar());
            PositionedNode<?> main = new PositionedNode<>().width(layout.getMainWidth()).height(layout.getWindowHeight() - 2 * SettingsLayout.WINDOW_PADDING);
            main.columnGap(SettingsLayout.GAP);
            PositionedNode<?> header = new PositionedNode<>().fullWidth().height(SettingsLayout.CONTROL_HEIGHT).rowGap(SettingsLayout.GAP);
            tab.buildHeader(header);
            header.addChild(new CloseButton(this::closeShell).size(SettingsLayout.CONTROL_HEIGHT, SettingsLayout.CONTROL_HEIGHT));
            main.addChild(header);
            PositionedNode<?> contents = new PositionedNode<>().fullWidth().height(layout.getContentHeight());
            contents.columnGap(SettingsLayout.GAP);
            main.addChild(contents);
            body.addChild(main);
            tab.buildContents(contents);
        });
        window.center();
        root.addChild(window);
        tab.addPopups(root);
        UiNode colorPickerPopup = colorPicker.popupOrNull();
        if (colorPickerPopup != null) {
            root.addChild(colorPickerPopup);
        }
        return root;
    }

    private SettingsTab currentTab(SettingsTabContext context) {
        return switch (settingsState.tab().get()) {
            case TAB_PROFILES -> new ProfilesTab(context);
            case TAB_SETTINGS -> new GlobalSettingsTab(context);
            default -> {
                Module module = settingsState.openModule().get();
                yield module != null ? new ModuleDetailTab(context, module) : new ModuleGridTab(context);
            }
        };
    }

    @Override
    protected void onEscapePressed() {
        if (colorPicker.isOpen()) {
            colorPicker.close();
        }
        else if (settingsState.moduleReset().get()) {
            settingsState.moduleReset().set(false);
        }
        else if (settingsState.profilePopupOpen()) {
            settingsState.closeProfilePopups();
        }
        else if (settingsState.tab().get().equals(TAB_MODULES) && settingsState.openModule().get() != null) {
            settingsState.openModule().set(null);
        }
        else {
            closeShell();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_F && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0 && settingsState.tab().get().equals(TAB_MODULES)) {
            stateStore.requestFocus(settingsState.openModule().get() == null ? ModuleGridTab.SEARCH_NODE_ID : ModuleDetailTab.SEARCH_NODE_ID);
            return true;
        }
        return super.keyPressed(event);
    }

    private PositionedNode<?> buildSidebar() {
        CardNode sidebar = new CardNode().width(SettingsLayout.SIDEBAR_WIDTH).fullHeight();
        sidebar.setFillResolver(UiTheme::inputFill);
        sidebar.setNoBorder();
        sidebar.setContents(rail -> {
            rail.left(SIDEBAR_PADDING).right(SIDEBAR_PADDING).top(0).bottom(SIDEBAR_PADDING);
            rail.columnGap(UiMetrics.SPACE_XS);
            rail.addChild(new TextNode(() -> getTitle().getString())
                    .setBold(true)
                    .setTextAlign(0f, 0.5f)
                    .fullWidth()
                    .height(SettingsLayout.CONTROL_HEIGHT));
            // Zero-height row adds one more gap so the first nav item lines up with the content below the header.
            rail.addChild(new PositionedNode<>().fullWidth().height(0));
            rail.addChild(navButton("alumite.setting.shell.tab_modules", TAB_MODULES, ModUiTextures.SUNNY.getId()));
            rail.addChild(navButton("alumite.setting.shell.tab_profiles", TAB_PROFILES, ModUiTextures.EDIT.getId()));
            rail.addChild(navButton("alumite.setting.shell.tab_settings", TAB_SETTINGS, ModUiTextures.SETTINGS.getId()));
        });
        ButtonNode hudEditor = new ButtonNode(() -> UiTranslations.INSTANCE.get("alumite.setting.shell.edit_hud_layout"))
                .setVariant(ButtonNode.ButtonVariant.DEFAULT)
                .setOnPress(() -> leaveShell(() -> HudManager.INSTANCE.setEditMode(true)));
        hudEditor.left(SIDEBAR_PADDING).right(SIDEBAR_PADDING).bottom(SIDEBAR_PADDING).height(SettingsLayout.CONTROL_HEIGHT);
        sidebar.addChild(hudEditor);
        return sidebar;
    }

    private ButtonNode navButton(String translationKey, String tabKey, Identifier icon) {
        ButtonNode button = new ButtonNode(() -> UiTranslations.INSTANCE.get(translationKey))
                .setLeftIcon(icon)
                .setVariant(() -> settingsState.tab().get().equals(tabKey) ? ButtonNode.ButtonVariant.PRIMARY : ButtonNode.ButtonVariant.GHOST)
                .setOnPress(() -> {
                    lastTabKey = tabKey;
                    settingsState.openModule().set(null);
                    settingsState.tab().set(tabKey);
                });
        button.fullWidth().height(SettingsLayout.CONTROL_HEIGHT);
        return button;
    }

    private void closeShell() {
        leaveShell(() -> Minecraft.getInstance().setScreen(null));
    }

    /**
     * Goes back to the screen this one was opened from, or runs the fallback when there is none.
     *
     * @param whenStandalone runs when the shell was opened on its own, outside the HUD editor
     */
    private void leaveShell(Runnable whenStandalone) {
        if (returnToScreen != null) {
            HudManager.INSTANCE.markEditModeActive();
            Minecraft.getInstance().setScreen(returnToScreen);
        }
        else {
            whenStandalone.run();
        }
    }
}
