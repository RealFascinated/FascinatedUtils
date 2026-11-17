package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.gui.toast.Toast;
import cc.fascinated.fascinatedutils.mixin.GameRendererPostProcessorAccessorMixin;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRestriction;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class VibrancyModule extends Module {
    /**
     * GameRenderer post-processor id for this module (shell blur restore must skip this when the module is off).
     */
    public static final Identifier POST_EFFECT_ID = Identifier.fromNamespaceAndPath("alumite", "vibrancy");

    /**
     * Last-observed Iris shader-pack-in-use state. Null until first observed. Used to detect a
     * shader-pack enable/disable so the stale vibrancy PostChain can be rebuilt.
     */
    @Nullable
    private Boolean lastIrisPackInUse;

    public VibrancyModule() {
        super("Vibrancy", ModuleCategory.GENERAL);
        addRestriction(ModuleRestriction.when(this::isIrisShaderPackInUse, Component.translatable("alumite.module.vibrancy.restriction.shaders")));
    }

    @EventHandler
    private void onClientTickEvent(ClientTickEvent event) {
        Minecraft minecraftClient = event.minecraftClient();
        if (isUserEnabled() && isIrisShaderPackStateChange()) {
            boolean shadersOn = isIrisShaderPackInUse();
            Toast.show().message(Component.translatable(shadersOn ? "alumite.module.vibrancy.disabled_shaders" : "alumite.module.vibrancy.enabled_shaders").getString()).info();
            minecraftClient.reloadResourcePacks();
        }
        if (!isEnabled()) {
            disableVibrancyPostProcessor(minecraftClient.gameRenderer);
            return;
        }
        GameRenderer gameRenderer = minecraftClient.gameRenderer;
        if (gameRenderer != null && POST_EFFECT_ID.equals(gameRenderer.currentPostEffect())) {
            // Vanilla F4 toggles effectActive, which would switch vibrancy off.
            GameRendererPostProcessorAccessorMixin accessor = (GameRendererPostProcessorAccessorMixin) gameRenderer;
            if (!accessor.alumite$isPostProcessorEnabled()) {
                accessor.alumite$setPostProcessorEnabled(true);
            }
            return;
        }
        ensureVibrancyPostProcessor(gameRenderer);
    }

    /**
     * Detects a change in the Iris shader-pack-in-use state. Enabling or disabling a shader pack
     * swaps Iris's rendering pipeline, which leaves the cached vibrancy PostChain and its compiled
     * pipeline stale; the post effect then stops applying until the game is restarted.
     *
     * @return true when the in-use state changed since the last tick, signalling that a resource
     *         reload is needed to rebuild the stale PostChain and compiled pipeline
     */
    private boolean isIrisShaderPackStateChange() {
        boolean inUse = isIrisShaderPackInUse();
        Boolean previous = lastIrisPackInUse;
        lastIrisPackInUse = inUse;
        return previous != null && previous != inUse;
    }

    private boolean isIrisShaderPackInUse() {
        return FabricLoader.getInstance().isModLoaded("iris") && IrisApi.getInstance().isShaderPackInUse();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        Minecraft minecraftClient = Minecraft.getInstance();
        // Re-observe the Iris state from scratch so a pack toggled while disabled doesn't trigger a reload now.
        lastIrisPackInUse = null;
        if (isEnabled()) {
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
        accessor.alumite$setPostProcessor(POST_EFFECT_ID);
        accessor.alumite$setPostProcessorEnabled(true);
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
