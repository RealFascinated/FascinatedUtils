package cc.fascinated.fascinatedutils.renderer.pipeline;

import cc.fascinated.fascinatedutils.AlumiteMod;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Pipelines and vertex formats for the mod's custom GUI shapes. Plain fills, gradients and glyph quads use the vanilla
 * {@link RenderPipelines#GUI} and {@link RenderPipelines#GUI_TEXTURED} pipelines so they batch with vanilla elements;
 * only rounded rectangles need a custom shader.
 */
@UtilityClass
public class FascinatedUiPipelines {
    /**
     * Vertex layout of the rounded-rect shader. Every shape parameter travels in the vertex data so all rounded
     * rectangles share one pipeline and one texture setup and therefore one batch:
     * <ul>
     *     <li>{@code Position.z}: corner radius in local units (full float precision)</li>
     *     <li>{@code UV0}: vertex offset from the shape centre in local units</li>
     *     <li>{@code UV1}: shape half size in 1/8 local unit steps</li>
     *     <li>{@code UV2}: x is the corner mask, y is the ring stroke in 1/64 local unit steps</li>
     * </ul>
     */
    public static final VertexFormat ROUNDED_RECT_FORMAT = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).add("UV0", VertexFormatElement.UV0).add("Color", VertexFormatElement.COLOR).add("UV1", VertexFormatElement.UV1).add("UV2", VertexFormatElement.UV2).build();

    /**
     * Rounded fill or ring with per-vertex color (solid or vertical gradient).
     */
    public static final RenderPipeline ROUNDED_RECT = register("pipeline/rounded_rect", false);

    /**
     * Rounded fill that also multiplies in {@code Sampler0}, sampled across the shape.
     */
    public static final RenderPipeline ROUNDED_RECT_TEXTURED = register("pipeline/rounded_rect_textured", true);

    private static RenderPipeline register(String pipelinePath, boolean textured) {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET).withLocation(Identifier.fromNamespaceAndPath(AlumiteMod.MOD_ID, pipelinePath)).withVertexShader(Identifier.fromNamespaceAndPath(AlumiteMod.MOD_ID, "core/fui_rounded_rect")).withFragmentShader(Identifier.fromNamespaceAndPath(AlumiteMod.MOD_ID, "core/fui_rounded_rect")).withVertexFormat(ROUNDED_RECT_FORMAT, VertexFormat.Mode.QUADS).withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false)).withCull(false).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT));
        if (textured) {
            builder.withShaderDefine("TEXTURED").withSampler("Sampler0");
        }
        return RenderPipelines.register(builder.build());
    }
}
