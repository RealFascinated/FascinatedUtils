package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.systems.hud.layout.HudLayoutCanvas;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.debug.DebugOptionsScreen;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.NonNull;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class HudModuleWidgetsElement implements HudElement {
    public static final HudModuleWidgetsElement INSTANCE = new HudModuleWidgetsElement(HudManager.INSTANCE);

    private final HudManager hudManager;

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker tickCounter) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DebugOptionsScreen) {
            return;
        }

        ProfilerFiller profiler = Profiler.get();
        profiler.push("futils_hud_elements");
        // Real time, not game time: animations keep moving while the game is paused or ticking slowly.
        float deltaSeconds = tickCounter.getRealtimeDeltaTicks() / 20f;
        if (!(deltaSeconds >= 0f)) {
            deltaSeconds = 0f;
        }
        float canvasWidth = HudLayoutCanvas.width();
        float canvasHeight = HudLayoutCanvas.height();
        hudManager.renderHUD(graphics, canvasWidth, canvasHeight, Mth.clamp(deltaSeconds, 0f, 1f));
        profiler.pop();
    }
}
