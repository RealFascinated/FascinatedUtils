package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.mixin.GameRendererPostProcessorAccessorMixin;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class VibrancyModule extends Module {
    /**
     * GameRenderer post-processor id for this module (shell blur restore must skip this when the module is off).
     */
    public static final Identifier POST_EFFECT_ID = Identifier.fromNamespaceAndPath("fascinatedutils", "vibrancy");

    public VibrancyModule() {
        super("Vibrancy");
    }

    @EventHandler
    private void onClientTickEvent(ClientTickEvent event) {
        Minecraft client = event.minecraftClient();
        if (!isEnabled()) {
            return;
        }
        ensureVibrancyPostProcessor(client.gameRenderer);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        Minecraft minecraftClient = Minecraft.getInstance();
        if (enabled) {
            ensureVibrancyPostProcessor(minecraftClient.gameRenderer);
            return;
        }
        disableVibrancyPostProcessor(minecraftClient.gameRenderer);
    }

    private void ensureVibrancyPostProcessor(@Nullable GameRenderer gameRenderer) {
        if (gameRenderer == null) {
            return;
        }
        GameRendererPostProcessorAccessorMixin accessor = (GameRendererPostProcessorAccessorMixin) gameRenderer;
        if (POST_EFFECT_ID.equals(gameRenderer.currentPostEffect())) {
            return;
        }
        accessor.fascinatedutils$setPostProcessor(POST_EFFECT_ID);
        accessor.fascinatedutils$setPostProcessorEnabled(true);
    }

    private void disableVibrancyPostProcessor(@Nullable GameRenderer gameRenderer) {
        if (gameRenderer == null) {
            return;
        }
        Identifier current = gameRenderer.currentPostEffect();
        if (POST_EFFECT_ID.equals(current)) {
            gameRenderer.clearPostEffect();
        }
    }
}
