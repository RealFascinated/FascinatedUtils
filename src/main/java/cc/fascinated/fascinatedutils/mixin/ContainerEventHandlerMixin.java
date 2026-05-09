package cc.fascinated.fascinatedutils.mixin;

import cc.fascinated.fascinatedutils.gui2.screens.impl.ActionsOverlayScreen;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ContainerEventHandler.class)
public interface ContainerEventHandlerMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void alumite$onPauseMenuButtonClicked(MouseButtonEvent event, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (this instanceof PauseScreen && ActionsOverlayScreen.INSTANCE.mouseClicked(event)) {
            cir.setReturnValue(true);
        }
    }
}
