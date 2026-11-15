package cc.fascinated.fascinatedutils.debug.perf.tests.particle;

import cc.fascinated.fascinatedutils.common.culling.AbstractCullTask;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.ParticlesPerfScene;
import cc.fascinated.fascinatedutils.mixin.turboparticles.ParticleEngineAccessor;
import cc.fascinated.fascinatedutils.systems.turboparticles.ParticleCullTask;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * One particle snapshot publish plus one cull pass over the particles scene's live particles, on a private
 * {@link ParticleCullTask} so the live cull thread's state is not shared.
 */
public class ParticleCullPassPerfTest extends PerfTest {
    private ParticleCullTask cullTask;
    private ParticleEngineAccessor particleEngine;
    private Vec3 cameraPosition;

    public ParticleCullPassPerfTest() {
        super("turbo_particles.publish_and_cull", "ParticleCullTask publish from the engine groups plus cullPass", 20, 500);
    }

    @Override
    public void setup() {
        Minecraft minecraft = Minecraft.getInstance();
        cullTask = new ParticleCullTask(AbstractCullTask.createOcclusionCulling());
        particleEngine = (ParticleEngineAccessor) minecraft.particleEngine;
        cameraPosition = minecraft.gameRenderer.getMainCamera().position();
    }

    @Override
    public void runOnce() {
        cullTask.publishParticleCullSnapshotFromGroups(particleEngine.getParticles().values());
        cullTask.cullPass(cameraPosition);
    }

    @Override
    public void teardown() {
        cullTask = null;
        particleEngine = null;
    }

    @Override
    public List<String> sceneIds() {
        return List.of(ParticlesPerfScene.ID);
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.system("turboparticles"), PerfCoverage.mixin("turboparticles.ParticleEngineAccessor"));
    }
}
