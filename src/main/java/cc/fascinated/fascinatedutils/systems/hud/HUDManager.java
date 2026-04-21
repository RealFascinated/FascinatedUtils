package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.screens.HUDEditorScreen;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.modules.Module;
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
        initialized = true;
    }

    public void renderHUD(GuiRenderer renderer, float canvasWidth, float canvasHeight, float deltaSeconds, boolean editorMode) {
        if (this.editMode || Minecraft.getInstance().debugEntries.isOverlayVisible()) {
            return;
        }

        ProfilerFiller profiler = Profiler.get();

        long renderStart = System.nanoTime();
        for (HudModule widget : widgets.stream().filter(Module::isEnabled).toList()) {
            profiler.push("widget-" + widget.getId());

            Runnable draw = widget.prepareAndDraw(renderer, deltaSeconds, editorMode);
            if (draw == null) {
                widget.recordHudContentSkipped();
            }
            widget.applyHudAnchorToPosition(canvasWidth, canvasHeight);
            if (draw != null) {
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

    /**
     * Marks edit mode as active without opening a new screen. Use this when the editor screen is
     * already being shown (e.g. re-using a parent {@link HUDEditorScreen} instance after returning from
     * mod settings).
     */
    public void markEditModeActive() {
        this.editMode = true;
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

    public void saveAll() {
        ModConfig.profiles().saveActiveProfile();
    }

    public void loadAll() {
        ModConfig.profiles().loadActiveProfile();
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
