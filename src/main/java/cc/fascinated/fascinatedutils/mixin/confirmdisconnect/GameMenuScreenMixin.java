package cc.fascinated.fascinatedutils.mixin.confirmdisconnect;

import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(PauseScreen.class)
public abstract class GameMenuScreenMixin {
    @Shadow
    private @Nullable Button disconnectButton;

    @Unique
    private boolean awaitingConfirm = false;

    @Inject(method = "createPauseMenu", at = @At("RETURN"))
    private void fascinatedutils$wrapDisconnectWithConfirm(CallbackInfo ci) {
        if (!SettingsRegistry.INSTANCE.getSettings().getConfirmDisconnect().isEnabled() || Minecraft.getInstance().hasSingleplayerServer() || this.disconnectButton == null) {
            return;
        }

        try {
            Field onPressField = Button.class.getDeclaredField("onPress");
            onPressField.setAccessible(true);

            Button.OnPress original = (Button.OnPress) onPressField.get(this.disconnectButton);
            Component originalLabel = this.disconnectButton.getMessage();

            onPressField.set(this.disconnectButton, (Button.OnPress) btn -> {
                if (!this.awaitingConfirm) {
                    this.awaitingConfirm = true;
                    btn.setMessage(Component.translatable("fascinatedutils.confirm_disconnect").withColor(0xFF5555));
                    return;
                }
                btn.setMessage(originalLabel);
                this.awaitingConfirm = false;
                original.onPress(btn);
            });
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to wrap disconnect button", e);
        }
    }
}