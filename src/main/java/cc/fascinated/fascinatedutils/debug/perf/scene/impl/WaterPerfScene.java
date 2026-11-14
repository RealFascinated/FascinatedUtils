package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;
import cc.fascinated.fascinatedutils.systems.modules.impl.FogCustomizerModule;

import java.util.List;

/**
 * The camera inside a block of water, so the water fog path runs instead of the atmospheric one.
 */
public class WaterPerfScene extends PerfScene {
    private static final String WATER_VOLUME = "~-3 ~-3 ~-3 ~3 ~3 ~3";

    public WaterPerfScene() {
        super("water", "Camera submerged in a 7x7x7 block of water");
    }

    @Override
    public void setup(PerfSceneContext context) {
        context.command("fill " + WATER_VOLUME + " water");
    }

    @Override
    public void teardown(PerfSceneContext context) {
        context.command("fill " + WATER_VOLUME + " air replace water");
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("fogcustomizer.WaterFogModifierMixin"), PerfCoverage.module(FogCustomizerModule.class));
    }
}
