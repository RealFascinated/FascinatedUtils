package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.client.keybind.KeybindsWrapper;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.event.impl.mouse.MouseScrollEvent;
import cc.fascinated.fascinatedutils.event.impl.render.ClientFovEvent;
import cc.fascinated.fascinatedutils.event.impl.render.FirstPersonHeldItemRenderEvent;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import cc.fascinated.fascinatedutils.systems.modules.ModuleDefaults;
import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

@Getter
public class ZoomModule extends Module {
    private static final float ZOOM_LERP_STEP_PER_TICK = 0.45f;
    private static final float ZOOM_SCALE_EPSILON = 1e-4f;
    private static final float ZOOM_MAX_MAGNIFICATION = 15f;
    // Wheel zoom multiplies mag per notch so each step stays noticeable at high zoom (FOV ~ 1/mag).
    private static final float SCROLL_ZOOM_MAG_RATIO_PER_NOTCH = 1.12f;
    private static final float ZOOM_MOUSE_LOOK_SCALE_MIN = 0.02f;
    private final KeyMapping zoomKeyBinding = KeybindsWrapper.registerKeybind("key.fascinatedutils.zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, KeyMapping.Category.MISC);
    private final KeybindSetting zoomKeySetting = new KeybindSetting("zoom_key", () -> zoomKeyBinding);
    private final SliderSetting zoomLevel = SliderSetting.builder().id("zoom_level").defaultValue(5f).minValue(1f).maxValue(ZOOM_MAX_MAGNIFICATION).step(1f).build();
    private final BooleanSetting scrollToZoom = BooleanSetting.builder().id("scroll_to_zoom").defaultValue(true).build();
    private final BooleanSetting smoothZoom = BooleanSetting.builder().id("smooth_zoom").defaultValue(true).build();
    private int tempScrollZoomOffset;
    private float appliedZoomScale = 1f;
    private float appliedZoomScaleTickStart = 1f;

    public ZoomModule() {
        super("Zoom", ModuleCategory.MISC, ModuleDefaults.builder().defaultState(true).build());
        addSetting(zoomKeySetting);
        addSetting(zoomLevel);
        addSetting(scrollToZoom);
        addSetting(smoothZoom);
    }

    private static float magnificationWithScrollOffset(int zoomTimes, int scrollOffset) {
        float baseMagnification = Mth.clamp(zoomTimes, 1, (int) ZOOM_MAX_MAGNIFICATION);
        float fromScroll = (float) Math.pow(SCROLL_ZOOM_MAG_RATIO_PER_NOTCH, scrollOffset);
        return Math.max(1f, baseMagnification * fromScroll);
    }

    /**
     * Adjust vanilla FOV in degrees when this module is zooming.
     *
     * <p>Smooth zoom advances {@link #appliedZoomScale} once per client tick in the {@link ClientTickEvent} handler;
     * this method
     * applies the scale for rendering (including sub-tick interpolation when smooth zoom is on) so {@code getFov}
     * may be invoked many times per frame without advancing the zoom state.
     *
     * @param fovDegrees   FOV returned by vanilla {@code GameRenderer#getFov}
     * @param tickProgress partial tick passed to {@code GameRenderer#getFov}, used to interpolate FOV within a tick
     * @return FOV after zoom, or {@code fovDegrees} when this module is disabled or zoom is inactive
     */
    public float adjustFovDegrees(float fovDegrees, float tickProgress) {
        if (!isEnabled()) {
            return fovDegrees;
        }
        float displayScale = smoothZoom.isEnabled() ? Mth.lerp(Mth.clamp(tickProgress, 0f, 1f), appliedZoomScaleTickStart, appliedZoomScale) : appliedZoomScale;
        if (displayScale >= 1f - ZOOM_SCALE_EPSILON) {
            return fovDegrees;
        }
        return fovDegrees * displayScale;
    }

    /**
     * Multiplier for mouse yaw/pitch deltas while this module is narrowing FOV, so the same physical motion does not
     * sweep the scene faster than when un-zoomed (FOV is multiplied by {@link #appliedZoomScale}).
     *
     * @return {@code 1} when this module is disabled or not zoomed; otherwise {@link #appliedZoomScale} clamped to a
     * small positive minimum
     */
    public float zoomMouseLookScale() {
        if (!isEnabled()) {
            return 1f;
        }
        float raw = appliedZoomScale;
        if (raw >= 1f - ZOOM_SCALE_EPSILON) {
            return 1f;
        }
        return Mth.clamp(raw, ZOOM_MOUSE_LOOK_SCALE_MIN, 1f);
    }

    /**
     * Whether zoom is actively held (module on and zoom key down), including for first-person hand hiding and mouse
     * smoothing.
     *
     * @return true when this module is enabled and the zoom key is held
     */
    public boolean isActive() {
        return isEnabled() && zoomKeyBinding.isDown();
    }

    /**
     * Whether vanilla in-game mouse wheel handling (including hotbar slot changes) should be skipped while zoomed.
     *
     * @return true when this module is enabled, no screen is open, and zoom is held or FOV is still narrowed
     */
    public boolean shouldSuppressInGameMouseScrollForZoom() {
        if (!isEnabled()) {
            return false;
        }
        Minecraft minecraftClient = Minecraft.getInstance();
        if (minecraftClient == null || minecraftClient.screen != null) {
            return false;
        }
        if (zoomKeyBinding.isDown()) {
            return true;
        }
        return appliedZoomScale < 1f - ZOOM_SCALE_EPSILON;
    }

    /**
     * Mouse wheel nudge while zooming; no-op when scroll-to-zoom is disabled.
     *
     * @param verticalScroll vertical scroll delta from GLFW
     */
    public void onMouseScrollWhileInGame(double verticalScroll) {
        if (!isEnabled() || !scrollToZoom.isEnabled()) {
            return;
        }
        Minecraft minecraftClient = Minecraft.getInstance();
        if (minecraftClient == null || minecraftClient.screen != null) {
            return;
        }
        if (!zoomKeyBinding.isDown()) {
            return;
        }
        if (verticalScroll > 0.0) {
            tempScrollZoomOffset++;
        }
        else if (verticalScroll < 0.0) {
            tempScrollZoomOffset--;
        }
    }

    @EventHandler
    private void onFirstPersonHeldItemRender(FirstPersonHeldItemRenderEvent event) {
        if (isActive()) {
            event.cancel();
        }
    }

    @EventHandler
    private void onClientFov(ClientFovEvent event) {
        float adjusted = adjustFovDegrees(event.vanillaFovDegrees(), event.tickProgress());
        if (adjusted != event.vanillaFovDegrees()) {
            event.setFovDegrees(adjusted);
        }
    }

    @EventHandler
    private void onMouseScrollEvent(MouseScrollEvent event) {
        Minecraft minecraftClient = Minecraft.getInstance();
        if (minecraftClient == null || event.windowHandle() != minecraftClient.getWindow().handle()) {
            return;
        }
        onMouseScrollWhileInGame(event.verticalScroll());
        if (event.verticalScroll() != 0.0 && shouldSuppressInGameMouseScrollForZoom()) {
            event.cancel();
        }
    }

    @EventHandler
    private void onClientTickEvent(ClientTickEvent event) {
        if (!isEnabled()) {
            tempScrollZoomOffset = 0;
            appliedZoomScale = 1f;
            appliedZoomScaleTickStart = 1f;
            return;
        }
        appliedZoomScaleTickStart = appliedZoomScale;
        if (!zoomKeyBinding.isDown()) {
            // Do not carry wheel offset into the next zoom; decay was easy to miss and felt like sticky zoom.
            tempScrollZoomOffset = 0;
        }
        float targetScale = 1f;
        if (zoomKeyBinding.isDown()) {
            float magnification = magnificationWithScrollOffset(Mth.clamp(Math.round(zoomLevel.getValue().floatValue()), 1, (int) ZOOM_MAX_MAGNIFICATION), tempScrollZoomOffset);
            targetScale = 1f / magnification;
        }
        if (smoothZoom.isEnabled()) {
            appliedZoomScale = Mth.lerp(ZOOM_LERP_STEP_PER_TICK, appliedZoomScale, targetScale);
            if (Math.abs(appliedZoomScale - targetScale) < ZOOM_SCALE_EPSILON) {
                appliedZoomScale = targetScale;
            }
        }
        else {
            appliedZoomScale = targetScale;
        }
        if (appliedZoomScale >= 1f - ZOOM_SCALE_EPSILON) {
            appliedZoomScale = 1f;
        }
    }
}
