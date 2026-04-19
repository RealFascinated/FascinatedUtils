package cc.fascinated.fascinatedutils.gui.screens;

import cc.fascinated.fascinatedutils.common.ClientGuiUtils;
import cc.fascinated.fascinatedutils.gui.GuiDesignSpace;
import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.UiSounds;
import cc.fascinated.fascinatedutils.gui.core.InputEvent;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.input.UiCursorController;
import cc.fascinated.fascinatedutils.gui.modsettings.*;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.themes.fascinated.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FShellTabStripWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidgetHost;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class ModSettingsScreen extends WidgetScreen {

    private static final ModSettingsShellHitRegions EMPTY_SHELL_HIT = new ModSettingsShellHitRegions(new ModSettingsShellLayout.ShellBounds(0f, 0f, 0f, 0f), new ModSettingsShellLayout.ShellBounds(0f, 0f, 0f, 0f), new ModSettingsShellLayout.ShellBounds(0f, 0f, 0f, 0f), 0f, 0f, 0f, 0f);
    private final FWidgetHost root = new FWidgetHost();
    private final FWidgetHost topBarTabsHost = new FWidgetHost();
    private final FWidgetHost hudLayoutButtonHost = new FWidgetHost();
    private final IntConsumer setFocusId;
    private final FShellTabStripWidget topBarTabStrip;
    private final ModulesTabElement modulesTabElement;
    private final SettingsTabElement settingsTabElement;
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
    private final @Nullable Module navigateToModuleDetailOnOpen;
    private boolean navigateToModuleDetailApplied;

    public ModSettingsScreen(Component title, IntSupplier getFocusId, IntConsumer setFocusId) {
        this(title, getFocusId, setFocusId, null);
    }

    public ModSettingsScreen(Component title, IntSupplier getFocusId, IntConsumer setFocusId, @Nullable Module navigateToModuleDetailOnOpen) {
        super(title);
        this.navigateToModuleDetailOnOpen = navigateToModuleDetailOnOpen;
        this.setFocusId = setFocusId;
        root.setFocusSync(getFocusId, setFocusId);
        topBarTabStrip = new FShellTabStripWidget(this::onShellTabSelected);
        topBarTabsHost.setRoot(topBarTabStrip);
        modulesTabElement = new ModulesTabElement(this::onProfilesChanged, this::openHudLayoutEditor);
        settingsTabElement = new SettingsTabElement();
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
            ModConfig.loadLastShellContentTabKey().ifPresent(tabKey -> {
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
        float framebufferScaleX = UIScale.framebufferScaleX();
        float framebufferScaleY = UIScale.framebufferScaleY();
        float pointerScreenX = UIScale.hiResPointerX();
        float pointerScreenY = UIScale.hiResPointerY();
        Minecraft minecraftClient = Minecraft.getInstance();
        ProfilerFiller profiler = Profiler.get();
        profiler.push("futils_settings_screen");
        float deltaSeconds = minecraftClient.getDeltaTracker().getGameTimeDeltaTicks() / 20f;
        if (deltaSeconds <= 0f || Float.isNaN(deltaSeconds)) {
            deltaSeconds = partialTick / 20f;
        }
        float canvasWidth = UIScale.physicalWidth();
        float canvasHeight = UIScale.physicalHeight();
        ClientGuiUtils.unscaledProjection();
        try {
            GuiDesignSpace.begin(framebufferScaleX, framebufferScaleY);
            try {
                GuiRenderer guiRenderer = new GuiRenderer(graphics, FascinatedGuiTheme.INSTANCE);
                guiRenderer.begin(canvasWidth, canvasHeight);
                root.tickAnimations(deltaSeconds);
                topBarTabsHost.tickAnimations(deltaSeconds);
                hudLayoutButtonHost.tickAnimations(deltaSeconds);
                ModSettingsShellFrameResult frameResult = ModSettingsShellFrame.render(graphics, framebufferScaleX, framebufferScaleY, canvasWidth, canvasHeight, guiRenderer, getTitle().getString(), pointerScreenX, pointerScreenY, deltaSeconds, minecraftClient, topBarTabStrip, selectedShellTabKey(), root, topBarTabsHost, hudLayoutButtonHost, this::beforeShellBodyLayout);
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
                GuiDesignSpace.end();
            }
        } finally {
            ClientGuiUtils.scaledProjection();
            profiler.pop();
        }
    }

    @Override
    public void removed() {
        Minecraft minecraftClient = Minecraft.getInstance();
        if (minecraftClient != null) {
            UiCursorController.apply(minecraftClient.getWindow().handle(), UiPointerCursor.DEFAULT);
        }
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
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        float framebufferScaleX = UIScale.framebufferScaleX();
        float framebufferScaleY = UIScale.framebufferScaleY();
        GuiDesignSpace.begin(framebufferScaleX, framebufferScaleY);
        try {
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
        } finally {
            GuiDesignSpace.end();
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        float framebufferScaleX = UIScale.framebufferScaleX();
        float framebufferScaleY = UIScale.framebufferScaleY();
        GuiDesignSpace.begin(framebufferScaleX, framebufferScaleY);
        try {
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
        } finally {
            GuiDesignSpace.end();
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        float framebufferScaleX = UIScale.framebufferScaleX();
        float framebufferScaleY = UIScale.framebufferScaleY();
        GuiDesignSpace.begin(framebufferScaleX, framebufferScaleY);
        try {
            ModSettingsShellPointer.LayoutPoint pointer = ModSettingsShellPointer.layoutPointInShellSpace();
            float layoutX = pointer.layoutPositionX();
            float layoutY = pointer.layoutPositionY();
            topBarTabsHost.dispatchInput(new InputEvent.MouseMove(layoutX, layoutY));
            hudLayoutButtonHost.dispatchInput(new InputEvent.MouseMove(layoutX, layoutY));
            if (root.dispatchInput(new InputEvent.MouseMove(layoutX, layoutY))) {
                return true;
            }
            return super.mouseDragged(event, dragX, dragY);
        } finally {
            GuiDesignSpace.end();
        }
    }

    @Override
    public void mouseMoved(double mousePosX, double mousePosY) {
        float framebufferScaleX = UIScale.framebufferScaleX();
        float framebufferScaleY = UIScale.framebufferScaleY();
        GuiDesignSpace.begin(framebufferScaleX, framebufferScaleY);
        try {
            ModSettingsShellPointer.LayoutPoint pointer = ModSettingsShellPointer.layoutPointInShellSpace();
            float layoutX = pointer.layoutPositionX();
            float layoutY = pointer.layoutPositionY();
            topBarTabsHost.dispatchInput(new InputEvent.MouseMove(layoutX, layoutY));
            hudLayoutButtonHost.dispatchInput(new InputEvent.MouseMove(layoutX, layoutY));
            root.dispatchInput(new InputEvent.MouseMove(layoutX, layoutY));
        } finally {
            GuiDesignSpace.end();
        }
        super.mouseMoved(mousePosX, mousePosY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollAccum += (float) verticalAmount;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        float framebufferScaleX = UIScale.framebufferScaleX();
        float framebufferScaleY = UIScale.framebufferScaleY();
        GuiDesignSpace.begin(framebufferScaleX, framebufferScaleY);
        try {
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
        } finally {
            GuiDesignSpace.end();
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int codepoint = event.codepoint();
        if (codepoint >= 0 && codepoint <= 0xFFFF) {
            float framebufferScaleX = UIScale.framebufferScaleX();
            float framebufferScaleY = UIScale.framebufferScaleY();
            GuiDesignSpace.begin(framebufferScaleX, framebufferScaleY);
            try {
                boolean handled = root.dispatchInput(new InputEvent.CharType((char) codepoint));
                syncFocusFromRoot();
                if (handled) {
                    return true;
                }
            } finally {
                GuiDesignSpace.end();
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
                ModConfig.saveLastShellContentTabKey(tabKey);
            }
            return;
        }
        if (FShellTabStripWidget.TAB_KEY_MODULES.equals(tabKey)) {
            if (shellContentTab != ShellContentTab.MODULES) {
                shellContentTab = ShellContentTab.MODULES;
                bodyRebuildDirty = true;
                ModConfig.saveLastShellContentTabKey(tabKey);
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
        Minecraft.getInstance().setScreen(null);
    }

    private void syncFocusFromRoot() {
        setFocusId.accept(root.focusedId());
    }

    private void openHudLayoutEditor() {
        HUDManager.INSTANCE.setEditMode(true);
    }

    private enum ShellContentTab {
        MODULES, SETTINGS
    }
}
