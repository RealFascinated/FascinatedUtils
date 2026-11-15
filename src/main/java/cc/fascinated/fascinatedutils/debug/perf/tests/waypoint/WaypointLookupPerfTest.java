package cc.fascinated.fascinatedutils.debug.perf.tests.waypoint;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.systems.waypoint.Waypoint;
import cc.fascinated.fascinatedutils.systems.waypoint.WaypointRepository;

import java.util.List;

/**
 * The current-world waypoint lookup that the beam renderer and the label HUD element each run once per frame.
 */
public class WaypointLookupPerfTest extends PerfTest {
    private List<Waypoint> lastResult;

    public WaypointLookupPerfTest() {
        super("waypoints.current_world_lookup", "WaypointRepository.getForCurrentWorldKey as run twice per frame", 200, 20_000);
    }

    @Override
    public void runOnce() {
        lastResult = WaypointRepository.getForCurrentWorldKey();
    }

    @Override
    public List<String> sceneIds() {
        return List.of("idle");
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.system("waypoint"));
    }
}
