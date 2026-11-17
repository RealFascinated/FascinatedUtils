package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.systems.hud.layout.HudPanelMerger;

import cc.fascinated.fascinatedutils.systems.hud.layout.HudLayoutCanvas;

import cc.fascinated.fascinatedutils.systems.hud.panel.HudPanel;

import cc.fascinated.fascinatedutils.debug.perf.PerfMode;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.core.overlay.UiOverlay;
import cc.fascinated.fascinatedutils.gui.core.overlay.UiOverlays;
import cc.fascinated.fascinatedutils.gui.core.state.UiStateStore;
import cc.fascinated.fascinatedutils.gui.screens.impl.hudeditor.HudEditorScreen;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.hud.node.HudLayerNode;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HudManager {
    public static final HudManager INSTANCE = new HudManager();

    private static final PerfProbe.Section RENDER_SECTION = PerfProbe.INSTANCE.section("hud.render");

    private final List<HudPanel> widgets = new ArrayList<>();
    private final List<HudPanel> widgetsView = Collections.unmodifiableList(widgets);
    private final HudPanelMerger panelMerger = new HudPanelMerger();
    private final UiOverlay overlay = UiOverlays.INSTANCE.register(new UiOverlay(this::composeHud).recomposeEveryFrame(true), UiOverlays.ORDER_HUD);
    private float frameDeltaSeconds;
    @Getter
    private boolean editMode;
    private boolean initialized;
    @Getter
    private volatile long lastRenderNanos = 0L;
    // Layout fingerprint taken when the editor opens, so closing it only writes the profile if something changed.
    private long editSessionSignature;

    private HudManager() {
        FascinatedEventBus.INSTANCE.subscribe(this);
    }

    public void init() {
        if (initialized) {
            return;
        }
        widgets.addAll(ModuleRegistry.INSTANCE.getHudPanels());
        initialized = true;
    }

    /**
     * Draws the HUD through its overlay: composes every visible panel's node subtree, lays the panels out and
     * anchors them, merges touching panels and draws, all in one pass over the HUD canvas.
     *
     * @param graphics     the draw context to submit to
     * @param canvasWidth  HUD canvas width in UI pixels
     * @param canvasHeight HUD canvas height in UI pixels
     * @param deltaSeconds real seconds since the previous frame
     */
    public void renderHUD(GuiGraphicsExtractor graphics, float canvasWidth, float canvasHeight, float deltaSeconds) {
        if (this.editMode || Minecraft.getInstance().debugEntries.isOverlayVisible()) {
            return;
        }
        long renderStart = System.nanoTime();
        long renderAllocStart = PerfProbe.INSTANCE.beginAllocation();
        frameDeltaSeconds = deltaSeconds;
        overlay.render(graphics, canvasWidth, canvasHeight, 0f, 0f, deltaSeconds);
        lastRenderNanos = System.nanoTime() - renderStart;
        RENDER_SECTION.record(lastRenderNanos);
        RENDER_SECTION.endAllocation(renderAllocStart);
    }

    /**
     * Builds this frame's HUD tree. Panels are composed from scratch every frame and nothing is kept between frames;
     * a panel with nothing to show is skipped but still keeps its anchored position up to date.
     */
    private UiNode composeHud(UiStateStore stateStore) {
        float canvasWidth = HudLayoutCanvas.width();
        float canvasHeight = HudLayoutCanvas.height();
        HudLayerNode layer = new HudLayerNode(panelMerger);
        for (int widgetIndex = 0; widgetIndex < widgets.size(); widgetIndex++) {
            HudPanel widget = widgets.get(widgetIndex);
            if (!widget.shouldRenderHudPanel()) {
                continue;
            }
            widget.composeInto(layer, frameDeltaSeconds, canvasWidth, canvasHeight);
        }
        return layer;
    }

    /**
     * Lists the HUD panels that want to render this frame. Allocates a new list, so it is meant for the
     * perf tests that need a stable snapshot ({@code HudPanelMergerPerfTest}), not for per-frame code.
     *
     * @return a new list of the visible panels
     */
    public List<HudPanel> visibleWidgets() {
        return widgets.stream().filter(HudPanel::shouldRenderHudPanel).toList();
    }

    public void setEditMode(boolean editMode) {
        if (this.editMode == editMode) {
            return;
        }
        this.editMode = editMode;
        Minecraft client = Minecraft.getInstance();
        if (editMode) {
            editSessionSignature = layoutSignature();
            client.setScreen(new HudEditorScreen());
        }
        else {
            saveIfEditorChangedLayout();
            if (client.screen instanceof HudEditorScreen) {
                client.setScreen(null);
            }
        }
    }

    /**
     * Marks edit mode as active without opening a new screen. Use this when the editor screen is
     * already being shown (e.g. re-using a parent {@link HudEditorScreen} instance after returning from
     * mod settings).
     */
    public void markEditModeActive() {
        if (!this.editMode) {
            editSessionSignature = layoutSignature();
        }
        this.editMode = true;
    }

    public void clearEditModeAfterEditorRemoved() {
        if (!this.editMode) {
            return;
        }
        this.editMode = false;
        saveIfEditorChangedLayout();
    }

    /**
     * Lists every registered HUD panel.
     *
     * @return an unmodifiable live view of the panels
     */
    public List<HudPanel> getWidgets() {
        return widgetsView;
    }

    /**
     * Fingerprints what the HUD editor can change: each panel's layout state and its host's enabled flag.
     */
    private long layoutSignature() {
        long signature = widgets.size();
        for (HudPanel widget : widgets) {
            ModuleHudState state = widget.getHudState();
            signature = signature * 31L + Float.floatToIntBits(state.getScale());
            signature = signature * 31L + state.getHudAnchor().ordinal();
            signature = signature * 31L + Float.floatToIntBits(state.getAnchorOffsetX());
            signature = signature * 31L + Float.floatToIntBits(state.getAnchorOffsetY());
            signature = signature * 31L + (state.isBottomAnchorRightAligned() ? 1L : 0L);
            signature = signature * 31L + (widget.hudHostModule().isUserEnabled() ? 1L : 0L);
        }
        return signature;
    }

    private void saveIfEditorChangedLayout() {
        long signature = layoutSignature();
        if (signature != editSessionSignature) {
            editSessionSignature = signature;
            saveAll();
        }
    }

    /**
     * Writes the active profile to disk immediately. Call sparingly: it is synchronous file IO.
     */
    public void saveAll() {
        ModConfig.profiles().saveActiveProfile();
    }

    public void loadAll() {
        ModConfig.profiles().loadActiveProfile();
    }

    @EventHandler
    private void alumite$onClientStarted(ClientStartedEvent event) {
        loadAll();
    }

    @EventHandler
    private void alumite$onClientStopping(ClientStoppingEvent event) {
        if (PerfMode.isWorldScenario()) {
            return;
        }
        saveAll();
    }
}
