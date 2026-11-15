package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.debug.perf.PerfMode;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfAllocation;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import cc.fascinated.fascinatedutils.gui.render.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.render.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.screens.impl.hudeditor.HUDEditorScreen;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class HUDManager {
    public static final HUDManager INSTANCE = new HUDManager();

    private static final PerfProbe.Section RENDER_SECTION = PerfProbe.INSTANCE.section("hud.render");
    private static final String WIDGET_SECTION_PREFIX = "hud.widget.";
    private static final String WIDGET_PROFILER_PREFIX = "widget-";

    private final List<HudPanel> widgets = new ArrayList<>();
    private final Map<HudPanel, PerfProbe.Section> widgetSections = new IdentityHashMap<>();
    private final Map<HudPanel, String> widgetProfilerNames = new IdentityHashMap<>();
    private final HudPanelMerger panelMerger = new HudPanelMerger();
    private final List<HudPanel> drawablePanels = new ArrayList<>();
    private final List<Runnable> drawCallbacks = new ArrayList<>();
    private long[] prepareNanos = new long[0];
    private long[] prepareAllocationBytes = new long[0];
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
        widgets.addAll(ModuleRegistry.INSTANCE.getHudPanels());
        for (HudPanel widget : widgets) {
            widgetSections.put(widget, PerfProbe.INSTANCE.section(WIDGET_SECTION_PREFIX + widget.getId()));
            widgetProfilerNames.put(widget, WIDGET_PROFILER_PREFIX + widget.getId());
        }
        initialized = true;
    }

    public void renderHUD(GuiRenderer renderer, float canvasWidth, float canvasHeight, float deltaSeconds, boolean editorMode) {
        if (this.editMode || Minecraft.getInstance().debugEntries.isOverlayVisible()) {
            return;
        }

        ProfilerFiller profiler = Profiler.get();

        long renderStart = System.nanoTime();
        long renderAllocStart = PerfProbe.INSTANCE.beginAllocation();
        drawablePanels.clear();
        drawCallbacks.clear();
        if (prepareAllocationBytes.length < widgets.size()) {
            prepareAllocationBytes = new long[widgets.size()];
            prepareNanos = new long[widgets.size()];
        }
        for (int widgetIndex = 0; widgetIndex < widgets.size(); widgetIndex++) {
            HudPanel widget = widgets.get(widgetIndex);
            if (!widget.shouldRenderHudPanel()) {
                continue;
            }
            profiler.push(widgetProfilerNames.get(widget));
            long prepareStart = System.nanoTime();
            long prepareAllocationStart = PerfProbe.INSTANCE.beginAllocation();

            Runnable draw = widget.prepareAndDraw(renderer, deltaSeconds, editorMode);
            if (draw == null) {
                widget.recordHudContentSkipped();
            }
            widget.applyHudAnchorToPosition(canvasWidth, canvasHeight);
            long elapsed = System.nanoTime() - prepareStart;
            long prepareBytes = prepareAllocationStart < 0 ? -1 : PerfAllocation.INSTANCE.currentThreadBytes() - prepareAllocationStart;
            if (draw != null) {
                prepareAllocationBytes[drawablePanels.size()] = prepareBytes;
                prepareNanos[drawablePanels.size()] = elapsed;
                drawablePanels.add(widget);
                drawCallbacks.add(draw);
            }
            else {
                PerfProbe.Section widgetSection = widgetSections.get(widget);
                widgetSection.record(elapsed);
                widgetSection.endAllocation(prepareAllocationStart);
            }
            profiler.pop();
        }

        panelMerger.update(drawablePanels, !editorMode);

        for (int index = 0; index < drawablePanels.size(); index++) {
            HudPanel widget = drawablePanels.get(index);
            profiler.push(widgetProfilerNames.get(widget));
            long drawStart = System.nanoTime();
            long drawAllocationStart = PerfProbe.INSTANCE.beginAllocation();

            widget.hudHostModule().setSquaredCornerMask(widget.getHudState().getSquaredCornerMask());
            renderer.pushTranslate(Math.round(widget.getHudState().getPositionX()), Math.round(widget.getHudState().getPositionY()));
            renderer.pushScale(widget.getHudState().getScale());
            drawCallbacks.get(index).run();
            renderer.endRenderSegment();
            renderer.popScale();
            renderer.popTranslate();
            widget.hudHostModule().setSquaredCornerMask(RectCornerRoundMask.NONE);

            PerfProbe.Section widgetSection = widgetSections.get(widget);
            widgetSection.record(prepareNanos[index] + System.nanoTime() - drawStart);
            long prepareBytes = prepareAllocationBytes[index];
            if (drawAllocationStart >= 0 && prepareBytes >= 0) {
                // Start token shifted back by the prepare bytes, so the end reading covers prepare plus draw.
                widgetSection.endAllocation(drawAllocationStart - prepareBytes);
            }
            profiler.pop();
        }
        lastRenderNanos = System.nanoTime() - renderStart;
        RENDER_SECTION.record(lastRenderNanos);
        RENDER_SECTION.endAllocation(renderAllocStart);
    }

    /**
     * Lists the HUD panels that want to render this frame.
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

    public List<HudPanel> getWidgets() {
        return Collections.unmodifiableList(widgets);
    }

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
