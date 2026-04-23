package cc.fascinated.fascinatedutils.mixin.macosresolution;

import cc.fascinated.fascinatedutils.common.ClientUtils;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import com.mojang.blaze3d.opengl.GlBackend;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlBackend.class)
public class GlBackendMixin {

    @Inject(method = "setWindowHints", at = @At(value = "HEAD"))
    private void preSetWindowHints(CallbackInfo ci) {
        BooleanSetting reduceMacOSResolution = SettingsRegistry.INSTANCE.getSettings().getReduceMacOSResolution();

        if (ClientUtils.isMacOS() && reduceMacOSResolution.isEnabled()) {
            GLFW.glfwWindowHint(GLFW.GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW.GLFW_FALSE);
        }
    }
}