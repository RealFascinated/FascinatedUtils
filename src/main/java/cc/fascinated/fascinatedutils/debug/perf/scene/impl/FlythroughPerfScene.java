package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/**
 * The player flies straight along x at a constant speed through fresh terrain, so chunks load and entities and block
 * entities stream in while the scene is measured.
 */
public class FlythroughPerfScene extends PerfScene {
    public static final String ID = "flythrough";
    private Vec3 startPosition;

    public FlythroughPerfScene() {
        super(ID, "Constant-speed flight through fresh terrain, so chunks, entities and block entities stream in");
    }

    @Override
    public void setup(PerfSceneContext context) {
        startPosition = context.getMinecraft().player.position();
    }

    @Override
    public void onTick(PerfSceneContext context, int tick) {
        context.command("tp @s ~1.5 ~ ~");
    }

    @Override
    public void teardown(PerfSceneContext context) {
        context.command(String.format(Locale.ROOT, "tp @s %.3f %.3f %.3f", startPosition.x, startPosition.y, startPosition.z));
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.system("turboentities"), PerfCoverage.mixin("turboentities.ClientLevelTickMixin"), PerfCoverage.mixin("turboentities.LevelRendererMixin"), PerfCoverage.mixin("turboentities.BlockEntityRenderDispatcherMixin"));
    }
}
