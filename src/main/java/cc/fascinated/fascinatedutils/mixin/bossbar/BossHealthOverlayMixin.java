package cc.fascinated.fascinatedutils.mixin.bossbar;

import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.bossbar.BossbarModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void alumite$cancelVanillaBossBar(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (ModuleRegistry.INSTANCE.getModule(BossbarModule.class).filter(Module::isEnabled).isPresent()) {
            ci.cancel();
        }
    }
}
