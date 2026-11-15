package cc.fascinated.fascinatedutils.mixin;

import cc.fascinated.fascinatedutils.common.FrameCounter;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.gui.core.UIScale;
import cc.fascinated.fascinatedutils.gui.screens.WidgetScreen;
import cc.fascinated.fascinatedutils.gui.toast.ToastManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

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

        float uiWidth = UIScale.uiWidth();
        float uiHeight = UIScale.uiHeight();
        // Real-time delta: the game-time delta is 0 while a singleplayer game is paused, which froze toasts and animations.
        float deltaSeconds = deltaTracker.getRealtimeDeltaTicks() / 20f;
        int mouseX = (int) minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
        int mouseY = (int) minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());

        gameRenderState.guiRenderState.reset();
        GuiGraphicsExtractor drawContext = new GuiGraphicsExtractor(minecraft, gameRenderState.guiRenderState, mouseX, mouseY);

        if (minecraft.screen instanceof WidgetScreen widgetScreen) {
            int scale = minecraft.getWindow().getGuiScale();
            widgetScreen.renderCustom(drawContext, mouseX * scale, mouseY * scale, deltaTracker.getRealtimeDeltaTicks());
        }

        cc.fascinated.fascinatedutils.gui.render.GuiRenderer toastRenderer = new cc.fascinated.fascinatedutils.gui.render.GuiRenderer(drawContext);
        toastRenderer.beginFrame(uiWidth, uiHeight);
        ToastManager.INSTANCE.render(toastRenderer, uiWidth, uiHeight, UIScale.uiPointerX(), UIScale.uiPointerY(), deltaSeconds);
        toastRenderer.endFrame();
        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(Objects.requireNonNull(minecraft.getMainRenderTarget().getDepthTexture()), 1.0);
        guiRenderer.render(fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
        ALUMITE$OVERLAY_PASS.end(overlayStart);
    }
}
