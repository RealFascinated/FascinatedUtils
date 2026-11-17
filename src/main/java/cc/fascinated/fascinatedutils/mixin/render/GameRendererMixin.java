package cc.fascinated.fascinatedutils.mixin.render;

import cc.fascinated.fascinatedutils.common.timing.FrameCounter;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.gui.core.overlay.UiOverlays;
import cc.fascinated.fascinatedutils.gui.core.UiScale;
import cc.fascinated.fascinatedutils.gui.screens.WidgetScreen;
import cc.fascinated.fascinatedutils.gui.toast.ToastManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Unique
    private static final PerfProbe.Section ALUMITE$OVERLAY_PASS = PerfProbe.INSTANCE.section("gui.overlay_pass");

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private GuiRenderer guiRenderer;

    @Shadow
    @Final
    private GameRenderState gameRenderState;

    @Shadow
    @Final
    private FogRenderer fogRenderer;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER))
    private void alumite$onRenderGui(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo callbackInfo) {
        long overlayStart = ALUMITE$OVERLAY_PASS.begin();
        FrameCounter.getInstance().onFrame();

        float uiWidth = UiScale.uiWidth();
        float uiHeight = UiScale.uiHeight();
        // Real-time delta: the game-time delta is 0 while a singleplayer game is paused, which froze toasts and animations.
        float deltaSeconds = deltaTracker.getRealtimeDeltaTicks() / 20f;
        int mouseX = (int) minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
        int mouseY = (int) minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());

        GuiGraphicsExtractor drawContext = new GuiGraphicsExtractor(minecraft, gameRenderState.guiRenderState, mouseX, mouseY);

        if (minecraft.screen instanceof WidgetScreen widgetScreen) {
            int scale = minecraft.getWindow().getGuiScale();
            widgetScreen.renderCustom(drawContext, mouseX * scale, mouseY * scale, deltaTracker.getRealtimeDeltaTicks());
        }

        ToastManager.INSTANCE.render(drawContext, uiWidth, uiHeight, UiScale.uiPointerX(), UiScale.uiPointerY(), deltaSeconds);
        // Every overlay has rendered this frame; input arriving before the next one sees them as live.
        UiOverlays.INSTANCE.endFrame();
        if (alumite$hasQueuedGui(gameRenderState.guiRenderState)) {
            GpuTexture depthTexture = minecraft.getMainRenderTarget().getDepthTexture();
            if (depthTexture != null) {
                RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(depthTexture, 1.0);
            }
            guiRenderer.render(fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
        }
        ALUMITE$OVERLAY_PASS.end(overlayStart);
    }

    /**
     * Reports whether anything was queued for this overlay pass, so an idle frame skips the depth clear and the GUI
     * render entirely.
     *
     * @param guiRenderState the render state the overlay pass filled
     * @return {@code true} if any element, text, item or picture-in-picture entry is queued
     */
    @Unique
    private static boolean alumite$hasQueuedGui(GuiRenderState guiRenderState) {
        boolean[] queued = new boolean[1];
        guiRenderState.forEachElement(element -> queued[0] = true, GuiRenderState.TraverseRange.ALL);
        guiRenderState.forEachText(text -> queued[0] = true);
        guiRenderState.forEachItem(item -> queued[0] = true);
        guiRenderState.forEachPictureInPicture(pip -> queued[0] = true);
        return queued[0];
    }
}
