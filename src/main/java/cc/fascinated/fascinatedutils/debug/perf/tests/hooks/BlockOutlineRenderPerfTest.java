package cc.fascinated.fascinatedutils.debug.perf.tests.hooks;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.renderer.world.WorldBoxFaces;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.blockoutline.BlockOutlineModule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.List;
import java.util.Optional;

/**
 * The block color part of BlockOutlineMixin's render hook: the enabled and block color gates, the resolved block
 * color, and the six box faces emitted for a full block. The vertices go to a counting sink, so no render pass or
 * buffer is needed. The mixin's camera-relative bounds math and the buffer batch flush are not included.
 */
public class BlockOutlineRenderPerfTest extends PerfTest {
    private final PoseStack.Pose pose = new PoseStack().last();
    private final CountingVertexConsumer consumer = new CountingVertexConsumer();
    private BlockOutlineModule module;
    private boolean originalEnabled;
    private boolean originalShowBlockColor;

    public BlockOutlineRenderPerfTest() {
        super("hook.blockoutline.render", "BlockOutlineModule block color gates, color resolve and WorldBoxFaces.emitBoxFaces for a full block", 50, 2_000);
    }

    @Override
    public void setup() {
        module = ModuleRegistry.INSTANCE.getModule(BlockOutlineModule.class).orElseThrow();
        originalEnabled = module.isEnabled();
        originalShowBlockColor = module.getShowBlockColor().getValue();
        module.setEnabled(true);
        module.getShowBlockColor().setValue(true);
    }

    @Override
    public void runOnce() {
        Optional<BlockOutlineModule> outlineModule = ModuleRegistry.INSTANCE.getModule(BlockOutlineModule.class);
        if (outlineModule.isEmpty() || !outlineModule.get().isEnabled() || !outlineModule.get().getShowBlockColor().isEnabled()) {
            return;
        }
        int argb = outlineModule.get().getBlockColor().getResolvedArgb();
        WorldBoxFaces.emitBoxFaces(consumer, pose, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, argb, true);
    }

    @Override
    public void teardown() {
        if (module != null) {
            module.getShowBlockColor().setValue(originalShowBlockColor);
            module.setEnabled(originalEnabled);
        }
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("blockoutline.BlockOutlineMixin"), PerfCoverage.module(BlockOutlineModule.class), PerfCoverage.renderer("WorldBoxFaces"));
    }

    /**
     * A vertex consumer that discards every vertex and attribute but counts the vertices, so the emit cost is measured
     * without a buffer growing across iterations.
     */
    private static class CountingVertexConsumer implements VertexConsumer {
        private int vertexCount;

        @Override
        public VertexConsumer addVertex(float posX, float posY, float posZ) {
            vertexCount++;
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int argb) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float uCoordinate, float vCoordinate) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int firstCoordinate, int secondCoordinate) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int firstCoordinate, int secondCoordinate) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }
    }
}
