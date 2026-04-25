package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import lombok.Getter;
import java.util.Set;

@Getter
public class BlurModule extends Module {

    private static final Set<String> DISABLED_SCREENS = Set.of(
            "net.irisshaders.iris.gui.screen.ShaderPackScreen",
            "net.minecraft.client.gui.screens.LevelLoadingScreen"
    );

    private final SliderSetting blurStrength = SliderSetting.builder().id("blur_strength")
            .defaultValue(3f)
            .minValue(1f)
            .maxValue(10f)
            .step(0.5f)
            .build();

    private final BooleanSetting animationEnabled = BooleanSetting.builder().id("animation_enabled")
            .defaultValue(true)
            .build();

    private final SliderSetting animationSpeed = SliderSetting.builder().id("animation_speed")
            .defaultValue(750f)
            .minValue(100f)
            .maxValue(2000f)
            .step(50f)
            .valueFormatter((value) -> value.intValue() + "ms")
            .build();

    // progress [0,1] driven per render frame; fadeIn is set each frame by ScreenBlurMixin
    private float progress = 0f;
    private boolean fadeIn = false;
    // tracks whether blurBeforeThisStratum() was already invoked this frame
    private boolean blurApplied = false;

    public BlurModule() {
        super("Blur", ModuleCategory.GENERAL);
        addSetting(blurStrength);
        addSetting(animationEnabled);
        addSetting(animationSpeed);
        animationEnabled.addSubSetting(animationSpeed);
    }

    /**
     * Called at the start of each render frame (GameRenderer.extract HEAD) to assume no screen is blurred
     * until {@link #onBlurDetected()} confirms one is.
     */
    public void beginFrame() {
        fadeIn = false;
        blurApplied = false;
    }

    /**
     * Called by the screen mixin when a screen invokes its blur background extraction, signalling that
     * the blur should be fading in this frame.
     */
    public void onBlurDetected() {
        fadeIn = true;
    }

    /**
     * Advances the animated progress toward 1 (fade-in) or 0 (fade-out) based on real-time delta seconds.
     * Called at the end of each render frame (GameRenderer.extract TAIL).
     *
     * @param deltaSeconds real elapsed seconds since last frame
     */
    public void advanceAnimation(float deltaSeconds) {
        if (!isEnabled()) {
            progress = 0f;
            return;
        }
        float target = fadeIn ? 1f : 0f;
        if (animationEnabled.isEnabled()) {
            float step = deltaSeconds * 1000f / animationSpeed.getValue().floatValue();
            if (progress < target) {
                progress = Math.min(1f, progress + step);
            } else {
                progress = Math.max(0f, progress - step);
            }
        } else {
            progress = target;
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            progress = 0f;
        }
    }

    /**
     * Returns {@code true} when the given screen class name is in the disabled list and should
     * never receive blur (e.g. screens that already render their own opaque background).
     *
     * @param screenClassName fully-qualified class name of the current screen
     * @return whether blur should be suppressed for this screen
     */
    public boolean isDisabledScreen(String screenClassName) {
        return DISABLED_SCREENS.contains(screenClassName);
    }

    /**
     * Records that {@code blurBeforeThisStratum()} has been called for this frame, preventing
     * a second call which would throw {@link IllegalStateException}.
     */
    public void markBlurApplied() {
        blurApplied = true;
    }
}
