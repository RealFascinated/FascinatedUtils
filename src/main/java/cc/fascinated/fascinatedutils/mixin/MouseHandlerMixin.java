package cc.fascinated.fascinatedutils.mixin;

import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.mouse.MouseClickEvent;
import cc.fascinated.fascinatedutils.event.impl.mouse.MouseScrollEvent;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.ZoomModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo callbackInfo) {
        long clientWindowHandle = Minecraft.getInstance().getWindow().handle();
        if (handle != clientWindowHandle) {
            return;
        }
        MouseClickEvent event = new MouseClickEvent(handle, rawButtonInfo, action);
        FascinatedEventBus.INSTANCE.postCancellable(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils(long handle, double xoffset, double yoffset, CallbackInfo callbackInfo) {
        long clientWindowHandle = Minecraft.getInstance().getWindow().handle();
        if (handle != clientWindowHandle) {
            return;
        }
        MouseScrollEvent event = new MouseScrollEvent(handle, xoffset, yoffset);
        FascinatedEventBus.INSTANCE.postCancellable(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }
}
