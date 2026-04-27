package cc.fascinated.fascinatedutils.mixin.tab;

import cc.fascinated.fascinatedutils.common.PingColors;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.TabModule;
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
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
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    @Unique
    private static final int TAB_LIST_MS_PING_EXTRA_COLUMN_PX = 45;

    @Unique
    private static net.minecraft.network.chat.Component parseMiniMessage(String miniMessageText) {
        Component adventure = MINI_MESSAGE.deserialize(miniMessageText == null ? "" : miniMessageText);
        return MinecraftClientAudiences.of().asNative(adventure);
    }

    @ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 13))
    private int fascinatedutils$tabListPingColumnWidthWhenMs(int original) {
        Optional<TabModule> tabModuleOptional = ModuleRegistry.INSTANCE.getModule(TabModule.class);
        if (tabModuleOptional.isEmpty() || !tabModuleOptional.get().isEnabled() || tabModuleOptional.get().getPingMode().getValue() != TabModule.PingMode.MILLISECONDS) {
            return original;
        }
        return original + TAB_LIST_MS_PING_EXTRA_COLUMN_PX;
    }

    @Inject(method = "extractPingIcon", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$tabPingMode(GuiGraphicsExtractor graphics, int slotWidth, int xo, int yo, PlayerInfo info, CallbackInfo callbackInfo) {
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
                String formatted = "%sms".formatted(latency);
                int textX = slotWidth + xo - textRenderer.width(formatted) - 13;
                textX += 12;
                int color = tabModuleOptional.get().getColoredPing().getValue() ? PingColors.getPingColor(latency) : 0xFFFFFFFF;
                String miniMessageText = "<color:#" + String.format("%06X", color & 0xFFFFFF) + ">" + formatted + "</color>";
                graphics.text(textRenderer, parseMiniMessage(miniMessageText), textX, yo, color, true);
            }
        }
    }
}
