package cc.fascinated.fascinatedutils.debug.perf.scene;

import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * One measured in-world situation. The world runner flies the player to a fixed camera, calls {@link #setup}, lets the
 * scene settle, records live timings for the measure window while calling {@link #onTick} every tick, runs the world
 * tests that declared this scene, then calls {@link #teardown}. Results are named {@code scene.<id>/<section>}.
 *
 * <p>To add a scene: subclass this, then add one {@code register(...)} line in {@link PerfSceneRegistry}.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PerfScene {
    private final String id;
    private final String description;

    /**
     * Prepares the scene after the camera is in place. Queue commands, hold keys or open screens through the context.
     *
     * @param context the running scene's helpers
     */
    public void setup(PerfSceneContext context) {
    }

    /**
     * Called every client tick while the scene settles and while it is measured.
     *
     * @param context the running scene's helpers
     * @param tick    ticks since setup finished, counting from 0
     */
    public void onTick(PerfSceneContext context, int tick) {
    }

    /**
     * Undoes the scene before the next one starts. Keys held and screens opened through the context are released
     * automatically after this.
     *
     * @param context the running scene's helpers
     */
    public void teardown(PerfSceneContext context) {
    }

    /**
     * How long the scene settles before measuring.
     *
     * @param scenario the running scenario
     * @return ticks to wait
     */
    public int settleTicks(PerfScenario scenario) {
        return scenario.settleTicks();
    }

    /**
     * How long the scene is measured.
     *
     * @param scenario the running scenario
     * @return ticks to record
     */
    public int measureTicks(PerfScenario scenario) {
        return scenario.measureTicks();
    }

    /**
     * The inventory ids this scene really drives, as checked by {@link PerfCoverage}.
     *
     * @return coverage ids such as {@code mixin:zoom.CameraMixin} or {@code module:zoom}
     */
    public List<String> covers() {
        return List.of();
    }
}
