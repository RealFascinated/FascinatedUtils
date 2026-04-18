package cc.fascinated.fascinatedutils.common.color;

import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import meteordevelopment.orbit.EventHandler;

/**
 * Global rainbow color that cycles through the hue spectrum once per second.
 * All HUD elements with rainbow enabled share this single color source.
 */
public class RainbowColors {
    private static final float CYCLE_SPEED_DEGREES_PER_TICK = 360f / 80f;
    private static final SettingColor current = new SettingColor(255, 0, 0, 255);
    private static float hue;

    public static void init() {
        FascinatedEventBus.INSTANCE.subscribe(RainbowColors.class);
    }

    /**
     * Returns the current global rainbow color.
     *
     * @return the current SettingColor (shared instance, do not mutate)
     */
    public static SettingColor currentColor() {
        return current;
    }

    @EventHandler
    private static void onTick(ClientTickEvent event) {
        hue = (hue + CYCLE_SPEED_DEGREES_PER_TICK) % 360f;
        SettingColor fromHue = SettingColor.fromHsv(hue, 1f, 1f);
        current.setRed(fromHue.getRed());
        current.setGreen(fromHue.getGreen());
        current.setBlue(fromHue.getBlue());
    }
}
