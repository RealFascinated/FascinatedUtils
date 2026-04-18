package cc.fascinated.fascinatedutils.mixin;

import cc.fascinated.fascinatedutils.common.FrameCounter;
import cc.fascinated.fascinatedutils.gui.screens.WidgetScreen;
import cc.fascinated.fascinatedutils.mixininterface.IGameRenderer;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.HurtcamModule;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.Optional;

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
        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(Objects.requireNonNull(minecraft.getMainRenderTarget().getDepthTexture()), 1.0);
        guiRenderer.render(fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true, require = 0)
    private void fascinatedutils$cancelHurtcamTilt(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo callbackInfo) {
        Optional<HurtcamModule> module = ModuleRegistry.INSTANCE.getModule(HurtcamModule.class);
        if (module.isPresent()) {
            HurtcamModule hurtcamModule = module.get();
            if (hurtcamModule.isEnabled() && hurtcamModule.getCancelHurtcamAnimation().isEnabled()) {
                callbackInfo.cancel();
            }
        }
    }
}
