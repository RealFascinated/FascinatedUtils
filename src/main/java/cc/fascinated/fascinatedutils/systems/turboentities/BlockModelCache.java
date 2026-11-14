package cc.fascinated.fascinatedutils.systems.turboentities;

import cc.fascinated.fascinatedutils.mixininterface.BlockModelRenderStateCopy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.BlockModelSet;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.world.level.block.state.BlockState;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Keeps the resolved model of block displays between frames. Resolving uses a fixed random seed and a context without a
 * position, so the result only depends on the block state until resources are reloaded.
 */
public class BlockModelCache {
    private final Map<BlockState, BlockModelRenderState> resolved = new IdentityHashMap<>();
    private BlockModelSet modelSet;
    private BlockDisplayContext displayContext;

    public int size() {
        return resolved.size();
    }

    /**
     * Fills the target with the resolved model of a block state, resolving it only the first time.
     *
     * @param resolver the vanilla resolver used on a miss
     * @param target   the render state to fill
     * @param state    the displayed block state
     * @param context  the display context the resolver is called with
     */
    public void resolve(BlockModelResolver resolver, BlockModelRenderState target, BlockState state, BlockDisplayContext context) {
        BlockModelSet currentModelSet = Minecraft.getInstance().getModelManager().getBlockModelSet();
        if (currentModelSet != modelSet || context != displayContext) {
            resolved.clear();
            modelSet = currentModelSet;
            displayContext = context;
        }
        BlockModelRenderState cached = resolved.get(state);
        if (cached == null) {
            cached = new BlockModelRenderState();
            resolver.update(cached, state, context);
            resolved.put(state, cached);
        }
        ((BlockModelRenderStateCopy) target).alumite$copyFrom(cached);
    }
}
