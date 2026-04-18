package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.screens.HUDEditorScreen;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class HUDManager {
    public static final HUDManager INSTANCE = new HUDManager();

    private final List<HudModule> widgets = new ArrayList<>();
    private final List<HudModule> visibleWidgets = new ArrayList<>();
    private boolean editMode;
    private boolean initialized;
    private volatile long lastRenderNanos = 0L;

    private HUDManager() {
        FascinatedEventBus.INSTANCE.subscribe(this);
    }

    public void init() {
        if (initialized) {
            return;
        }
        widgets.addAll(ModuleRegistry.INSTANCE.getHudModules());
        rebuildVisibleWidgets();
        initialized = true;
    }

    public void renderHUD(GuiRenderer renderer, float deltaSeconds) {
        if (editMode || Minecraft.getInstance().debugEntries.isOverlayVisible()) {
            return;
        }

        ProfilerFiller profiler = Profiler.get();

        float canvasWidth = UIScale.logicalWidth();
        float canvasHeight = UIScale.logicalHeight();

        long renderStart = System.nanoTime();
        for (HudModule widget : visibleWidgets) {
            profiler.push("widget-" + widget.getId());

            Runnable draw = widget.prepareAndDraw(renderer, deltaSeconds, false);
            if (draw == null) {
                widget.recordHudContentSkipped();
            }
            else {
                widget.applyHudAnchorToPosition(canvasWidth, canvasHeight);
                renderer.pushTranslate(widget.getHudState().getPositionX(), widget.getHudState().getPositionY());
                renderer.pushScale(widget.getHudState().getScale());
                draw.run();
                renderer.endRenderSegment();
                renderer.popScale();
                renderer.popTranslate();
            }
            profiler.pop();
        }
        lastRenderNanos = System.nanoTime() - renderStart;
    }

    public void setEditMode(boolean editMode) {
        if (this.editMode == editMode) {
            return;
        }
        this.editMode = editMode;
        Minecraft client = Minecraft.getInstance();
        if (editMode) {
            client.setScreen(new HUDEditorScreen());
        }
        else {
            saveAll();
            if (client.screen instanceof HUDEditorScreen) {
                client.setScreen(null);
            }
        }
    }

    public void clearEditModeAfterEditorRemoved() {
        if (!this.editMode) {
            return;
        }
        this.editMode = false;
        saveAll();
    }

    public List<HudModule> getWidgets() {
        return Collections.unmodifiableList(widgets);
    }

    public void setWidgetVisible(HudModule widget, boolean visible, boolean persistImmediately) {
        if (widget.getHudState().isVisible() == visible) {
            return;
        }
        widget.getHudState().setVisible(visible);
        widget.setEnabled(visible);
        onWidgetVisibilityStateChanged(widget);
        if (persistImmediately) {
            saveAll();
        }
    }

    public void onWidgetVisibilityStateChanged(HudModule widget) {
        if (widget.getHudState().isVisible()) {
            if (!visibleWidgets.contains(widget)) {
                visibleWidgets.add(widget);
            }
            return;
        }
        visibleWidgets.remove(widget);
    }

    public void saveAll() {
        ModConfig.saveAllModuleSettings();
    }

    public void loadAll() {
        ModConfig.loadAllModuleSettings();
        rebuildVisibleWidgets();
    }

    private void rebuildVisibleWidgets() {
        visibleWidgets.clear();
        for (HudModule widget : widgets) {
            if (widget.getHudState().isVisible()) {
                visibleWidgets.add(widget);
            }
        }
    }

    @EventHandler
    private void fascinatedutils$onClientStarted(ClientStartedEvent event) {
        loadAll();
    }

    @EventHandler
    private void fascinatedutils$onClientStopping(ClientStoppingEvent event) {
        saveAll();
    }
}
