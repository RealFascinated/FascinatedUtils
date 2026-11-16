package cc.fascinated.fascinatedutils.mixin.turboentities;

import cc.fascinated.fascinatedutils.mixininterface.BlockModelRenderStateCopy;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(BlockModelRenderState.class)
public abstract class BlockModelRenderStateMixin implements BlockModelRenderStateCopy {
    @Shadow
    private List<BlockStateModelPart> modelParts;
    @Shadow
    private Matrix4fc transformation;
    @Shadow
    private RenderType renderType;
    @Shadow
    private SpecialModelRenderer<?> specialRenderer;
    @Shadow
    private Matrix4fc specialRendererTransformation;
    @Shadow
    private IntList tintLayers;
    @Shadow
    private RandomSource randomSource;

    @Override
    public void alumite$copyFrom(BlockModelRenderState source) {
        BlockModelRenderStateMixin other = (BlockModelRenderStateMixin) (Object) source;
        modelParts = other.modelParts;
        transformation = other.transformation;
        renderType = other.renderType;
        specialRenderer = other.specialRenderer;
        specialRendererTransformation = other.specialRendererTransformation;
        // Own copy: vanilla clears a state's tint list in place, which would wipe the cached entry for every display.
        tintLayers = other.tintLayers == null ? null : new IntArrayList(other.tintLayers);
        randomSource = other.randomSource;
    }
}
