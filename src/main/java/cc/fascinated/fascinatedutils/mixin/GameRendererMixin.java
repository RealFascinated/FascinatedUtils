package cc.fascinated.fascinatedutils.mixin;

import cc.fascinated.fascinatedutils.common.FrameCounter;
import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.screens.WidgetScreen;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.toast.ToastManager;
import cc.fascinated.fascinatedutils.mixininterface.IGameRenderer;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements IGameRenderer {

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
    private void fascinatedutils$onRenderGui(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo callbackInfo) {
        FrameCounter.getInstance().onFrame();

        if (!(minecraft.screen instanceof WidgetScreen widgetScreen)) {
            return;
        }
        gameRenderState.guiRenderState.reset();
        int mouseX = (int) minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
        int mouseY = (int) minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
        GuiGraphicsExtractor drawContext = new GuiGraphicsExtractor(minecraft, gameRenderState.guiRenderState, mouseX, mouseY);
        int scale = minecraft.getWindow().getGuiScale();
        widgetScreen.renderCustom(drawContext, mouseX * scale, mouseY * scale, deltaTracker.getGameTimeDeltaTicks());
        // Render toasts on top of every screen
        float uiWidth = UIScale.uiWidth();
        float uiHeight = UIScale.uiHeight();
        float deltaSeconds = deltaTracker.getGameTimeDeltaTicks() / 20f;
        cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer toastRenderer =
                new cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer(drawContext, FascinatedGuiTheme.INSTANCE);
        toastRenderer.begin(uiWidth, uiHeight);
        ToastManager.INSTANCE.render(toastRenderer, uiWidth, uiHeight,
                UIScale.uiPointerX(), UIScale.uiPointerY(), deltaSeconds);
        toastRenderer.end();
        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(Objects.requireNonNull(minecraft.getMainRenderTarget().getDepthTexture()), 1.0);
        guiRenderer.render(fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
    }
}
