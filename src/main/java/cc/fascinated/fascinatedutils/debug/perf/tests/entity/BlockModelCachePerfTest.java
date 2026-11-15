package cc.fascinated.fascinatedutils.debug.perf.tests.entity;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.systems.turboentities.BlockModelCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * BlockModelCache.resolve for block display states in the entities scene. A private cache is used, so the live cache
 * is not touched. After warm-up every call is a cache hit that copies the resolved model into the target render state.
 */
public class BlockModelCachePerfTest extends PerfTest {
    private final BlockModelCache blockModelCache = new BlockModelCache();
    private final BlockModelRenderState target = new BlockModelRenderState();
    private BlockModelResolver resolver;
    private BlockDisplayContext displayContext;
    private List<BlockState> states = List.of();
    private int nextState;
    private int sink;

    public BlockModelCachePerfTest() {
        super("turbo_entities.block_model_cache", "BlockModelCache.resolve lookups for block display states (hits after warm-up)", 20, 2_000);
    }

    @Override
    public void setup() {
        resolver = new BlockModelResolver(Minecraft.getInstance().getModelManager());
        displayContext = BlockDisplayContext.create();
        states = List.of(
                Blocks.STONE.defaultBlockState(),
                Blocks.OAK_STAIRS.defaultBlockState(),
                Blocks.GRASS_BLOCK.defaultBlockState(),
                Blocks.OAK_LOG.defaultBlockState(),
                Blocks.GLASS.defaultBlockState(),
                Blocks.OAK_FENCE.defaultBlockState(),
                Blocks.COBBLESTONE_WALL.defaultBlockState(),
                Blocks.LANTERN.defaultBlockState()
        );
    }

    @Override
    public void runOnce() {
        BlockState state = states.get(nextState);
        nextState = (nextState + 1) % states.size();
        blockModelCache.resolve(resolver, target, state, displayContext);
        sink += target.isEmpty() ? 0 : 1;
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.system("turboentities"));
    }
}
