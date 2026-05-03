package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.screens.ModSettingsScreen;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.toast.ToastManager;
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
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DebugOptionsScreen || minecraft.screen instanceof ModSettingsScreen) {
            return;
        }

        GuiRenderer guiRenderer = new GuiRenderer(graphics, FascinatedGuiTheme.INSTANCE);
        ProfilerFiller profiler = Profiler.get();
        profiler.push("futils_hud_elements");
        float deltaSeconds = tickCounter.getGameTimeDeltaTicks() / 20f;
        if (deltaSeconds <= 0f || Float.isNaN(deltaSeconds)) {
            deltaSeconds = 1f / 20f;
        }
        float canvasWidth = HudLayoutCanvas.width();
        float canvasHeight = HudLayoutCanvas.height();
        float uiWidth = UIScale.uiWidth();
        float uiHeight = UIScale.uiHeight();
        guiRenderer.begin(canvasWidth, canvasHeight);
        hudManager.renderHUD(guiRenderer, canvasWidth, canvasHeight, Mth.clamp(deltaSeconds, 0f, 1f), false);
        // Toasts are rendered after the screen by GameRendererMixin when a WidgetScreen is open,
        // so only render them here when no screen is obscuring them.
        if (!(minecraft.screen instanceof cc.fascinated.fascinatedutils.gui.screens.WidgetScreen)) {
            ToastManager.INSTANCE.render(guiRenderer, uiWidth, uiHeight,
                    UIScale.uiPointerX(), UIScale.uiPointerY(), deltaSeconds);
        }
        guiRenderer.end();
        profiler.pop();
    }
}
