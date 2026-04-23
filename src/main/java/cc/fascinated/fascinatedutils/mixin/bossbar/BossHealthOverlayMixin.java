package cc.fascinated.fascinatedutils.mixin.bossbar;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.BossbarModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {

    /**
     * Matches vanilla {@code BossHealthOverlay.BAR_HEIGHT} — vertical space reclaimed per boss row when the bar is not
     * drawn, used to nudge stacked titles upward without moving the first boss title.
     */
    @Unique
    private static final int fascinatedutils$BOSS_BAR_HEIGHT = 5;

    @Unique
    private int fascinatedutils$bossTitleCallIndex;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void fascinatedutils$resetBossTitleCallIndex(CallbackInfo callbackInfo) {
        fascinatedutils$bossTitleCallIndex = 0;
    }

    @Inject(method = "extractBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/world/BossEvent;)V", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$hideOnlyBossBar(GuiGraphicsExtractor graphics, int x, int y, BossEvent event, CallbackInfo ci) {
        Optional<BossbarModule> moduleOptional = ModuleRegistry.INSTANCE.getModule(BossbarModule.class);
        if (moduleOptional.isEmpty() || !moduleOptional.get().isEnabled()) {
            return;
        }
        if (moduleOptional.get().getHideBossHealth().isEnabled()) {
            ci.cancel();
        }
    }

    @ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"), index = 3)
    private int fascinatedutils$raiseStackedBossTitlesWhenBarHidden(int y) {
        int callIndex = fascinatedutils$bossTitleCallIndex++;
        Optional<BossbarModule> moduleOptional = ModuleRegistry.INSTANCE.getModule(BossbarModule.class);
        if (moduleOptional.isEmpty() || !moduleOptional.get().isEnabled()) {
            return y;
        }
        if (!moduleOptional.get().getHideBossHealth().isEnabled()) {
            return y;
        }
        if (callIndex == 0) {
            return y;
        }
        return y - callIndex * fascinatedutils$BOSS_BAR_HEIGHT;
    }
}
