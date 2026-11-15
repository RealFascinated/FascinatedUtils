package cc.fascinated.fascinatedutils.mixin.tab;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.TabModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(PlayerTabOverlay.class)
public class PlayerListHudMixin {

    @Unique
    private static final int TAB_LIST_MS_PING_EXTRA_COLUMN_PX = 45;

    @ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 13))
    private int alumite$tabListPingColumnWidthWhenMs(int original) {
        Optional<TabModule> tabModuleOptional = ModuleRegistry.INSTANCE.getModule(TabModule.class);
        if (tabModuleOptional.isEmpty() || !tabModuleOptional.get().isEnabled() || tabModuleOptional.get().getPingMode().getValue() != TabModule.PingMode.MILLISECONDS) {
            return original;
        }
        return original + TAB_LIST_MS_PING_EXTRA_COLUMN_PX;
    }

    @Inject(method = "extractPingIcon", at = @At("HEAD"), cancellable = true)
    private void alumite$tabPingMode(GuiGraphicsExtractor graphics, int slotWidth, int xo, int yo, PlayerInfo info, CallbackInfo callbackInfo) {
        Optional<TabModule> tabModuleOptional = ModuleRegistry.INSTANCE.getModule(TabModule.class);
        if (tabModuleOptional.isEmpty() || !tabModuleOptional.get().isEnabled()) {
            return;
        }
        switch (tabModuleOptional.get().getPingMode().getValue()) {
            case BARS -> {
            }
            case NONE -> callbackInfo.cancel();
            case MILLISECONDS -> {
                callbackInfo.cancel();
                Font textRenderer = Minecraft.getInstance().font;
                int latency = info.getLatency();
                Component pingLabel = tabModuleOptional.get().millisecondsPingComponent(latency);
                int textX = slotWidth + xo - textRenderer.width(pingLabel) - 1;
                graphics.text(textRenderer, pingLabel, textX, yo, tabModuleOptional.get().pingColor(latency), true);
            }
        }
    }
}
