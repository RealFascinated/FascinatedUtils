package cc.fascinated.fascinatedutils.mixin.performance;

import cc.fascinated.fascinatedutils.common.ClientUtils;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {
    @Shadow
    private int framebufferWidth;

    @Shadow
    private int framebufferHeight;

    @Inject(method = "refreshFramebufferSize", at = @At(value = "RETURN"))
    private void fascinatedutils$afterUpdateFrameBufferSize(CallbackInfo ci) {
        BooleanSetting reduceMacOSResolution = SettingsRegistry.INSTANCE.getSettings().getReduceMacOSResolution();

        // prevents mis-scaled startup screen
        if (ClientUtils.isMacOS() && reduceMacOSResolution.isEnabled()) {
            framebufferWidth /= 2;
            framebufferHeight /= 2;
        }
    }
}
