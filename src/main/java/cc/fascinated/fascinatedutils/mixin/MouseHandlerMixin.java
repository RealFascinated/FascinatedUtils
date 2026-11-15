package cc.fascinated.fascinatedutils.mixin;

import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.mouse.MouseClickEvent;
import cc.fascinated.fascinatedutils.event.impl.mouse.MouseScrollEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void alumite$onButton(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo callbackInfo) {
        if (!alumite$isClientWindow(handle)) {
            return;
        }
        MouseClickEvent event = new MouseClickEvent(handle, rawButtonInfo, action);
        FascinatedEventBus.INSTANCE.postCancellable(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void alumite$onScroll(long handle, double xoffset, double yoffset, CallbackInfo callbackInfo) {
        if (!alumite$isClientWindow(handle)) {
            return;
        }
        MouseScrollEvent event = new MouseScrollEvent(handle, xoffset, yoffset);
        FascinatedEventBus.INSTANCE.postCancellable(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Unique
    private static boolean alumite$isClientWindow(long handle) {
        return handle == Minecraft.getInstance().getWindow().handle();
    }
}
