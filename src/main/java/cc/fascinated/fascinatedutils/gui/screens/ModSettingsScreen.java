package cc.fascinated.fascinatedutils.gui.screens;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.UiSounds;
import cc.fascinated.fascinatedutils.gui.core.InputEvent;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.input.UiCursorController;
import cc.fascinated.fascinatedutils.gui.modsettings.FModulesTabElement;
import cc.fascinated.fascinatedutils.gui.modsettings.FSettingsTabElement;
import cc.fascinated.fascinatedutils.gui.modsettings.ModSettingsShellFrame;
import cc.fascinated.fascinatedutils.gui.modsettings.ModSettingsShellFrameResult;
import cc.fascinated.fascinatedutils.gui.modsettings.ModSettingsShellHitRegions;
import cc.fascinated.fascinatedutils.gui.modsettings.ModSettingsShellLayout;
import cc.fascinated.fascinatedutils.gui.modsettings.ModSettingsShellPointer;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FShellTabStripWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidgetHost;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.HudLayoutCanvas;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

public class ModSettingsScreen extends WidgetScreen {

    private enum ShellContentTab {
        MODULES, SETTINGS
    }
    private static final ModSettingsShellHitRegions EMPTY_SHELL_HIT = new ModSettingsShellHitRegions(new ModSettingsShellLayout.ShellBounds(0f, 0f, 0f, 0f), new ModSettingsShellLayout.ShellBounds(0f, 0f, 0f, 0f), new ModSettingsShellLayout.ShellBounds(0f, 0f, 0f, 0f), 0f, 0f, 0f, 0f);
    private final FWidgetHost root = new FWidgetHost();
    private final FWidgetHost topBarTabsHost = new FWidgetHost();
    private final FWidgetHost hudLayoutButtonHost = new FWidgetHost();
    private final IntConsumer setFocusId;
    private final FShellTabStripWidget topBarTabStrip;
    private final FModulesTabElement modulesTabElement;
    private final FSettingsTabElement settingsTabElement;
    private final @Nullable Module navigateToModuleDetailOnOpen;
    private float scrollAccum;
    private boolean appliedPersistedShellTab;
    private ShellContentTab shellContentTab = ShellContentTab.MODULES;
    private ModSettingsShellHitRegions shellHitRegions = EMPTY_SHELL_HIT;
    private float lastBodyW;
    private float lastBodyH;
    private float bodyCacheWidth = -1f;
    private float bodyCacheHeight = -1f;
    private ShellContentTab bodyCacheShellTab = ShellContentTab.MODULES;
    private boolean bodyRebuildDirty = true;

    private boolean navigateToModuleDetailApplied;
    @Nullable
    private final Screen returnToScreen;

    public ModSettingsScreen(Component title, IntSupplier getFocusId, IntConsumer setFocusId) {
        this(title, getFocusId, setFocusId, null, (Screen) null);
    }

    public ModSettingsScreen(Component title, IntSupplier getFocusId, IntConsumer setFocusId, @Nullable Module navigateToModuleDetailOnOpen) {
        this(title, getFocusId, setFocusId, navigateToModuleDetailOnOpen, (Screen) null);
    }

    public ModSettingsScreen(Component title, IntSupplier getFocusId, IntConsumer setFocusId, @Nullable Module navigateToModuleDetailOnOpen, boolean returnToHudEditor) {
        this(title, getFocusId, setFocusId, navigateToModuleDetailOnOpen, (Screen) null);
    }

    public ModSettingsScreen(Component title, IntSupplier getFocusId, IntConsumer setFocusId, @Nullable Module navigateToModuleDetailOnOpen, @Nullable Screen returnToScreen) {
        super(title);
        this.returnToScreen = returnToScreen;
        this.navigateToModuleDetailOnOpen = navigateToModuleDetailOnOpen;
        this.setFocusId = setFocusId;
        root.setFocusSync(getFocusId, setFocusId);
        topBarTabStrip = new FShellTabStripWidget(this::onShellTabSelected);
        topBarTabsHost.setRoot(topBarTabStrip);
        modulesTabElement = new FModulesTabElement(this::onProfilesChanged, this::openHudLayoutEditor);
        settingsTabElement = new FSettingsTabElement();
    }

    public void renderBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderCustom(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!appliedPersistedShellTab) {
            ShellContentTab previousTab = shellContentTab;
            ModConfig.uiState().loadLastShellContentTabKey().ifPresent(tabKey -> {
                switch (tabKey) {
                    case FShellTabStripWidget.TAB_KEY_SETTINGS -> shellContentTab = ShellContentTab.SETTINGS;
                    case FShellTabStripWidget.TAB_KEY_PROFILES -> shellContentTab = ShellContentTab.MODULES;
                    case FShellTabStripWidget.TAB_KEY_MODULES -> shellContentTab = ShellContentTab.MODULES;
                }
            });
            if (shellContentTab != previousTab) {
                bodyRebuildDirty = true;
            }
            topBarTabStrip.setSelectedKey(selectedShellTabKey());
            appliedPersistedShellTab = true;
        }
        applyNavigateToModuleDetailIfPending();
        float pointerScreenX = UIScale.uiPointerX();
        float pointerScreenY = UIScale.uiPointerY();
        Minecraft minecraftClient = Minecraft.getInstance();
        ProfilerFiller profiler = Profiler.get();
        profiler.push("futils_settings_screen");
        float deltaSeconds = minecraftClient.getDeltaTracker().getGameTimeDeltaTicks() / 20f;
        if (deltaSeconds <= 0f || Float.isNaN(deltaSeconds)) {
            deltaSeconds = partialTick / 20f;
        }
        float canvasWidth = UIScale.uiWidth();
        float canvasHeight = UIScale.uiHeight();
        try {
            GuiRenderer guiRenderer = new GuiRenderer(graphics, FascinatedGuiTheme.INSTANCE);
            guiRenderer.begin(canvasWidth, canvasHeight);
            root.tickAnimations(deltaSeconds);
            topBarTabsHost.tickAnimations(deltaSeconds);
            hudLayoutButtonHost.tickAnimations(deltaSeconds);
            ModSettingsShellFrameResult frameResult = ModSettingsShellFrame.render(graphics, canvasWidth, canvasHeight, guiRenderer, getTitle().getString(), pointerScreenX, pointerScreenY, deltaSeconds, minecraftClient, topBarTabStrip, selectedShellTabKey(), root, topBarTabsHost, hudLayoutButtonHost, this::beforeShellBodyLayout);
            float hudCanvasWidth = HudLayoutCanvas.width();
            float hudCanvasHeight = HudLayoutCanvas.height();
            guiRenderer.begin(hudCanvasWidth, hudCanvasHeight);
            try {
                HUDManager.INSTANCE.renderHUD(guiRenderer, hudCanvasWidth, hudCanvasHeight, Mth.clamp(deltaSeconds, 0f, 1f), true);
            } finally {
                guiRenderer.end();
            }
            shellHitRegions = frameResult.hitRegions();
            lastBodyW = shellHitRegions.bodyWidth();
            lastBodyH = shellHitRegions.bodyHeight();
            float pointerX = frameResult.pointerLayoutX();
            float pointerY = frameResult.pointerLayoutY();
            topBarTabsHost.dispatchInput(new InputEvent.MouseMove(pointerX, pointerY));
            hudLayoutButtonHost.dispatchInput(new InputEvent.MouseMove(pointerX, pointerY));
            root.dispatchInput(new InputEvent.MouseMove(pointerX, pointerY));
            UiPointerCursor topBarCursor = topBarTabsHost.pointerCursorAt(pointerX, pointerY);
            UiPointerCursor hudLayoutCursor = hudLayoutButtonHost.pointerCursorAt(pointerX, pointerY);
            UiPointerCursor shellCursor = root.pointerCursorAt(pointerX, pointerY);
            UiPointerCursor topBarAreaCursor = topBarCursor != UiPointerCursor.DEFAULT ? topBarCursor : hudLayoutCursor != UiPointerCursor.DEFAULT ? hudLayoutCursor : shellCursor;
            UiCursorController.apply(minecraftClient.getWindow().handle(), topBarAreaCursor);
            if (scrollAccum != 0f) {
                root.dispatchInput(new InputEvent.MouseScroll(pointerX, pointerY, scrollAccum));
                scrollAccum = 0f;
            }
            setFocusId.accept(root.focusedId());
        } finally {
            profiler.pop();
        }
    }

    @Override
    public void removed() {
        Minecraft minecraftClient = Minecraft.getInstance();
        UiCursorController.apply(minecraftClient.getWindow().handle(), UiPointerCursor.DEFAULT);
        root.dispose();
        topBarTabsHost.dispose();
        hudLayoutButtonHost.dispose();
        modulesTabElement.reset();
        settingsTabElement.reset();
        bodyCacheWidth = -1f;
        bodyCacheHeight = -1f;
        bodyCacheShellTab = ShellContentTab.MODULES;
        bodyRebuildDirty = true;
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubled) {
        ModSettingsShellPointer.LayoutPoint pointer = ModSettingsShellPointer.layoutPointInShellSpace();
        float layoutX = pointer.layoutPositionX();
        float layoutY = pointer.layoutPositionY();
        if (shellHitRegions.close().contains(layoutX, layoutY) && event.button() == 0) {
            UiSounds.playButtonClick();
            closeModSettingsShell();
            return true;
        }
        if (shellHitRegions.topBarTabs().contains(layoutX, layoutY) && topBarTabsHost.dispatchInput(new InputEvent.MousePress(layoutX, layoutY, event.button()))) {
            syncFocusFromRoot();
            return true;
        }
        if (shellHitRegions.hudLayoutChip().contains(layoutX, layoutY) && hudLayoutButtonHost.dispatchInput(new InputEvent.MousePress(layoutX, layoutY, event.button()))) {
            syncFocusFromRoot();
            return true;
        }
        boolean handled = root.dispatchInput(new InputEvent.MousePress(layoutX, layoutY, event.button()));
        syncFocusFromRoot();
        if (handled) {
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        ModSettingsShellPointer.LayoutPoint pointer = ModSettingsShellPointer.layoutPointInShellSpace();
        float layoutX = pointer.layoutPositionX();
        float layoutY = pointer.layoutPositionY();
        if (shellHitRegions.topBarTabs().contains(layoutX, layoutY) && topBarTabsHost.dispatchInput(new InputEvent.MouseRelease(layoutX, layoutY, event.button()))) {
            syncFocusFromRoot();
            return true;
        }
        if (shellHitRegions.hudLayoutChip().contains(layoutX, layoutY) && hudLayoutButtonHost.dispatchInput(new InputEvent.MouseRelease(layoutX, layoutY, event.button()))) {
            syncFocusFromRoot();
            return true;
        }
        boolean handled = root.dispatchInput(new InputEvent.MouseRelease(layoutX, layoutY, event.button()));
        syncFocusFromRoot();
        if (handled) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        ModSettingsShellPointer.LayoutPoint pointer = ModSettingsShellPointer.layoutPointInShellSpace();
        float layoutX = pointer.layoutPositionX();
        float layoutY = pointer.layoutPositionY();
        topBarTabsHost.dispatchInput(new InputEvent.MouseMove(layoutX, layoutY));
        hudLayoutButtonHost.dispatchInput(new InputEvent.MouseMove(layoutX, layoutY));
        if (root.dispatchInput(new InputEvent.MouseMove(layoutX, layoutY))) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public void mouseMoved(double mousePosX, double mousePosY) {
        ModSettingsShellPointer.LayoutPoint pointer = ModSettingsShellPointer.layoutPointInShellSpace();
        float layoutX = pointer.layoutPositionX();
        float layoutY = pointer.layoutPositionY();
        topBarTabsHost.dispatchInput(new InputEvent.MouseMove(layoutX, layoutY));
        hudLayoutButtonHost.dispatchInput(new InputEvent.MouseMove(layoutX, layoutY));
        root.dispatchInput(new InputEvent.MouseMove(layoutX, layoutY));
        super.mouseMoved(mousePosX, mousePosY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollAccum += (float) verticalAmount;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        InputEvent.KeyPress keyPress = new InputEvent.KeyPress(event.key(), event.scancode(), event.modifiers());
        boolean handled = root.dispatchInput(keyPress);
        syncFocusFromRoot();
        if (handled) {
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            closeModSettingsShell();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int codepoint = event.codepoint();
        if (codepoint >= 0 && codepoint <= 0xFFFF) {
            boolean handled = root.dispatchInput(new InputEvent.CharType((char) codepoint));
            syncFocusFromRoot();
            if (handled) {
                return true;
            }
        }
        return super.charTyped(event);
    }

    private void beforeShellBodyLayout(float bodyWidth, float bodyHeight) {
        lastBodyW = bodyWidth;
        lastBodyH = bodyHeight;
        if (modSettingsBodyRequiresRebuild()) {
            root.setRoot(buildBody());
            commitModSettingsBodyCache();
        }
    }

    private boolean modSettingsBodyRequiresRebuild() {
        FWidget rootElement = root.root();
        if (rootElement == null) {
            return true;
        }
        if (lastBodyW != bodyCacheWidth || lastBodyH != bodyCacheHeight) {
            return true;
        }
        if (shellContentTab != bodyCacheShellTab) {
            return true;
        }
        return bodyRebuildDirty;
    }

    private void commitModSettingsBodyCache() {
        bodyCacheWidth = lastBodyW;
        bodyCacheHeight = lastBodyH;
        bodyCacheShellTab = shellContentTab;
        bodyRebuildDirty = false;
    }

    private FWidget buildBody() {
        return switch (shellContentTab) {
            case MODULES -> modulesTabElement;
            case SETTINGS -> settingsTabElement;
        };
    }

    private void onShellTabSelected(String tabKey) {
        if (FShellTabStripWidget.TAB_KEY_SETTINGS.equals(tabKey)) {
            if (shellContentTab != ShellContentTab.SETTINGS) {
                shellContentTab = ShellContentTab.SETTINGS;
                bodyRebuildDirty = true;
                ModConfig.uiState().saveLastShellContentTabKey(tabKey);
            }
            return;
        }
        if (FShellTabStripWidget.TAB_KEY_MODULES.equals(tabKey)) {
            if (shellContentTab != ShellContentTab.MODULES) {
                shellContentTab = ShellContentTab.MODULES;
                bodyRebuildDirty = true;
                ModConfig.uiState().saveLastShellContentTabKey(tabKey);
            }
        }
        topBarTabStrip.setSelectedKey(selectedShellTabKey());
    }

    private String selectedShellTabKey() {
        return switch (shellContentTab) {
            case MODULES -> FShellTabStripWidget.TAB_KEY_MODULES;
            case SETTINGS -> FShellTabStripWidget.TAB_KEY_SETTINGS;
        };
    }

    private void onProfilesChanged() {
        settingsTabElement.reset();
        bodyRebuildDirty = true;
    }

    private void applyNavigateToModuleDetailIfPending() {
        if (navigateToModuleDetailOnOpen == null || navigateToModuleDetailApplied) {
            return;
        }
        shellContentTab = ShellContentTab.MODULES;
        topBarTabStrip.setSelectedKey(selectedShellTabKey());
        modulesTabElement.navigateToModuleSettingsPage(navigateToModuleDetailOnOpen);
        navigateToModuleDetailApplied = true;
        bodyRebuildDirty = true;
    }

    private void closeModSettingsShell() {
        if (returnToScreen != null) {
            HUDManager.INSTANCE.markEditModeActive();
            Minecraft.getInstance().setScreen(returnToScreen);
        } else {
            Minecraft.getInstance().setScreen(null);
        }
    }

    private void syncFocusFromRoot() {
        setFocusId.accept(root.focusedId());
    }

    private void openHudLayoutEditor() {
        if (returnToScreen != null) {
            HUDManager.INSTANCE.markEditModeActive();
            Minecraft.getInstance().setScreen(returnToScreen);
        } else {
            HUDManager.INSTANCE.setEditMode(true);
        }
    }
}
