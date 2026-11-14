package cc.fascinated.fascinatedutils.mixininterface;

import net.minecraft.client.renderer.block.BlockModelRenderState;

public interface BlockModelRenderStateCopy {
    /**
     * Makes this render state hold the same resolved model as another one.
     *
     * @param source the fully resolved render state to copy
     */
    void alumite$copyFrom(BlockModelRenderState source);
}
