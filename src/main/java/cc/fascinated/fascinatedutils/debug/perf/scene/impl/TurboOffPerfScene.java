package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScenario;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;
import cc.fascinated.fascinatedutils.settings.Settings;

import java.util.List;

/**
 * Runs another scene with every Turbo feature switched off, so the same workload can be compared with Turbo on and
 * off. The Turbo settings are changed in memory only and restored exactly after the scene's teardown.
 */
public class TurboOffPerfScene extends PerfScene {
    private static final String TURBO_OFF_SUFFIX = "_turbo_off";
    private static final String TURBO_OFF_DESCRIPTION_SUFFIX = " (Turbo off)";

    private final PerfScene inner;
    private Boolean previousTurboEntities;
    private Boolean previousTurboParticles;
    private Boolean previousTurboWeather;

    /**
     * Wraps a scene.
     *
     * @param inner the scene to run with Turbo off
     */
    public TurboOffPerfScene(PerfScene inner) {
        super(inner.getId() + TURBO_OFF_SUFFIX, inner.getDescription() + TURBO_OFF_DESCRIPTION_SUFFIX);
        this.inner = inner;
    }

    @Override
    public void setup(PerfSceneContext context) {
        Settings settings = Settings.INSTANCE;
        BooleanSetting turboEntities = settings.getTurboEntities();
        BooleanSetting turboParticles = settings.getTurboParticles();
        BooleanSetting turboWeather = settings.getTurboWeather();
        previousTurboEntities = turboEntities.getValue();
        previousTurboParticles = turboParticles.getValue();
        previousTurboWeather = turboWeather.getValue();
        turboEntities.setValue(false);
        turboParticles.setValue(false);
        turboWeather.setValue(false);
        inner.setup(context);
    }

    @Override
    public void onTick(PerfSceneContext context, int tick) {
        inner.onTick(context, tick);
    }

    @Override
    public void teardown(PerfSceneContext context) {
        try {
            inner.teardown(context);
        } finally {
            restoreTurboSettings();
        }
    }

    @Override
    public int settleTicks(PerfScenario scenario) {
        return inner.settleTicks(scenario);
    }

    @Override
    public int measureTicks(PerfScenario scenario) {
        return inner.measureTicks(scenario);
    }

    /**
     * Declares no coverage: the wrapped scene already declares what it drives.
     *
     * @return an empty list
     */
    @Override
    public List<String> covers() {
        return List.of();
    }

    private void restoreTurboSettings() {
        if (previousTurboEntities == null) {
            // Setup never ran (the runner can tear down after a failure before setup), so nothing was changed.
            return;
        }
        Settings settings = Settings.INSTANCE;
        settings.getTurboEntities().setValue(previousTurboEntities);
        settings.getTurboParticles().setValue(previousTurboParticles);
        settings.getTurboWeather().setValue(previousTurboWeather);
        previousTurboEntities = null;
        previousTurboParticles = null;
        previousTurboWeather = null;
    }
}
