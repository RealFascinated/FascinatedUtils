package cc.fascinated.fascinatedutils.mixin.gui;

import cc.fascinated.fascinatedutils.gui.screens.impl.ActionsOverlay;
import cc.fascinated.fascinatedutils.systems.settings.Settings;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class GameMenuScreenMixin {
    @Inject(method = "createPauseMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/CommonComponents;disconnectButtonLabel(Z)Lnet/minecraft/network/chat/Component;", shift = At.Shift.BEFORE))
    private void alumite$addServerListRowBeforeDisconnect(CallbackInfo callbackInfo, @Local(ordinal = 0) GridLayout.RowHelper helper) {
        if (Settings.INSTANCE.getShowServerListInPauseMenu().isEnabled()) {
            PauseScreen self = (PauseScreen) (Object) this;
            Minecraft client = Minecraft.getInstance();
            helper.addChild(Button.builder(Component.translatable("alumite.setting.pause_menu.server_list"), button -> client.setScreen(new JoinMultiplayerScreen(self))).width(204).build(), 2);
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void alumite$drawPauseMenuOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ActionsOverlay.INSTANCE.render(graphics);
    }

}
