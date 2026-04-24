package cc.fascinated.fascinatedutils.renderer;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.fabricmc.loader.api.FabricLoader;

public class FascinatedWorldRenderTypes {

    private static final RenderPipeline WORLD_BEAM_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(FascinatedUtils.MOD_ID, "pipeline/world_beam"))
                    .build());

    /**
     * Iris-compatible replacement for {@code RenderTypes.debugFilledBox()}.
     *
     * <p>Uses a mod-namespaced pipeline so Iris does not attempt to override it with its own
     * shader programs, avoiding the "Missing program minecraft:pipeline/debug_filled_box"
     * error when shaders are active.
     */
    public static final RenderType WORLD_BEAM = RenderType.create(
            "fascinatedutils_world_beam",
            RenderSetup.builder(WORLD_BEAM_PIPELINE)
                    .sortOnUpload()
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .createRenderSetup());

    /**
     * Registers {@link #WORLD_BEAM_PIPELINE} with Iris so it is included in the override list
     * when a shader pack is active. No-op if Iris is not loaded.
     */
    public static void registerWithIris() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) {
            return;
        }
        IrisApi.getInstance().assignPipeline(WORLD_BEAM_PIPELINE, IrisProgram.BASIC);
    }
}
