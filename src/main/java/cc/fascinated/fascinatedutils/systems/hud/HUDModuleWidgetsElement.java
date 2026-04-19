package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.themes.fascinated.FascinatedGuiTheme;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.debug.DebugOptionsScreen;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.NonNull;

public class HUDModuleWidgetsElement implements HudElement {
    public static final HUDModuleWidgetsElement INSTANCE = new HUDModuleWidgetsElement(HUDManager.INSTANCE);

    private final HUDManager hudManager;

    private HUDModuleWidgetsElement(HUDManager hudManager) {
        this.hudManager = hudManager;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker tickCounter) {
        if (Minecraft.getInstance().screen instanceof DebugOptionsScreen) {
            return;
        }

        GuiRenderer guiRenderer = new GuiRenderer(graphics, FascinatedGuiTheme.INSTANCE);
        ProfilerFiller profiler = Profiler.get();
        profiler.push("futils_hud_elements");
        float deltaSeconds = tickCounter.getGameTimeDeltaTicks() / 20f;
        if (deltaSeconds <= 0f || Float.isNaN(deltaSeconds)) {
            deltaSeconds = 1f / 20f;
        }
        float canvasWidth = HudLayoutCanvas.width(graphics);
        float canvasHeight = HudLayoutCanvas.height(graphics);
        guiRenderer.begin(canvasWidth, canvasHeight);
        hudManager.renderHUD(guiRenderer, canvasWidth, canvasHeight, Mth.clamp(deltaSeconds, 0f, 1f));
        guiRenderer.end();
        profiler.pop();
    }
}
