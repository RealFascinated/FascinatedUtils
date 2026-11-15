package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Taps each of the mod's callback keybinds in turn (HUD editor, screenshots, waypoints), letting the opened screen
 * render before closing it again, so the keybind dispatch and each screen's open path run.
 */
public class KeybindsPerfScene extends PerfScene {
    public static final String ID = "keybinds";
    private static final List<String> CALLBACK_KEYS = List.of("key.alumite.settings_keybind", "key.alumite.screenshots_keybind", "key.alumite.waypoints_keybind");
    private static final int TICKS_PER_KEY = 20;
    private static final int CLOSE_AFTER_TICKS = 15;

    public KeybindsPerfScene() {
        super(ID, "Each callback keybind tapped in turn, its screen rendered, then closed");
    }

    @Override
    public void onTick(PerfSceneContext context, int tick) {
        int phase = tick % TICKS_PER_KEY;
        if (phase == 0) {
            String keyName = CALLBACK_KEYS.get(tick / TICKS_PER_KEY % CALLBACK_KEYS.size());
            context.clickKey(KeyMapping.get(keyName));
        } else if (phase == CLOSE_AFTER_TICKS) {
            close(context.getMinecraft());
        }
    }

    @Override
    public void teardown(PerfSceneContext context) {
        close(context.getMinecraft());
    }

    private void close(Minecraft minecraft) {
        if (HUDManager.INSTANCE.isEditMode()) {
            HUDManager.INSTANCE.setEditMode(false);
        }
        minecraft.setScreen(null);
    }

    @Override
    public List<String> covers() {
        return CALLBACK_KEYS.stream().map(PerfCoverage::keybind).toList();
    }
}
