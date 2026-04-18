package cc.fascinated.fascinatedutils.renderer;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class FascinatedUiPipelines {
    /**
     * Axis-aligned quad with white texture and per-vertex color (non-SDF textured path).
     */
    public static final RenderPipeline AXIS_TEX_COLOR = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET, RenderPipelines.GUI_TEXTURED_SNIPPET).withLocation(Identifier.fromNamespaceAndPath(FascinatedUtils.MOD_ID, "pipeline/axis_tex_color")).withVertexShader(Identifier.withDefaultNamespace("core/position_tex_color")).withFragmentShader(Identifier.fromNamespaceAndPath(FascinatedUtils.MOD_ID, "core/fui_axis_tex_color")).withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false)).withCull(false).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).withSampler("Sampler0").build());

    /**
     * Solid {@code POSITION} + {@code Color} quad ({@code pos_color} path).
     */
    public static final RenderPipeline SOLID_COLOR = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET, RenderPipelines.DEBUG_FILLED_SNIPPET).withLocation(Identifier.fromNamespaceAndPath(FascinatedUtils.MOD_ID, "pipeline/solid_color")).withVertexShader(Identifier.withDefaultNamespace("core/position_color")).withFragmentShader(Identifier.fromNamespaceAndPath(FascinatedUtils.MOD_ID, "core/fui_solid_color")).withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false)).withCull(false).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).build());

    /**
     * Rounded fill: all four corners filleted; radius comes from vertex alpha ({@link MeshRenderer#packArgbRadius}).
     */
    public static final RenderPipeline ROUNDED_RECT_ALL = registerRoundedPreset("pipeline/rounded_rect_all", "core/fui_rounded_rect_all");

    /**
     * Rounded fill: top two corners only.
     */
    public static final RenderPipeline ROUNDED_RECT_TOP = registerRoundedPreset("pipeline/rounded_rect_top", "core/fui_rounded_rect_top");

    /**
     * Rounded fill: bottom two corners only.
     */
    public static final RenderPipeline ROUNDED_RECT_BOTTOM = registerRoundedPreset("pipeline/rounded_rect_bottom", "core/fui_rounded_rect_bottom");

    /**
     * Rounded fill: left two corners only.
     */
    public static final RenderPipeline ROUNDED_RECT_LEFT = registerRoundedPreset("pipeline/rounded_rect_left", "core/fui_rounded_rect_left");

    /**
     * Rounded fill: right two corners only.
     */
    public static final RenderPipeline ROUNDED_RECT_RIGHT = registerRoundedPreset("pipeline/rounded_rect_right", "core/fui_rounded_rect_right");

    /**
     * Rounded rectangle fill using a per-corner radii LUT ({@code Sampler1}); used when the corner mask is not one of
     * the five presets, or when gradient alphas are not both fully opaque (vertex alpha carries radius on preset
     * paths).
     */
    public static final RenderPipeline ROUNDED_RECT_TEX_LUT = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET, RenderPipelines.GUI_TEXTURED_SNIPPET).withLocation(Identifier.fromNamespaceAndPath(FascinatedUtils.MOD_ID, "pipeline/rounded_rect_tex_lut")).withVertexShader(Identifier.withDefaultNamespace("core/position_tex_color")).withFragmentShader(Identifier.fromNamespaceAndPath(FascinatedUtils.MOD_ID, "core/fui_rounded_rect")).withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false)).withCull(false).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).withSampler("Sampler0").withSampler("Sampler1").build());

    private FascinatedUiPipelines() {
    }

    private static RenderPipeline registerRoundedPreset(String pipelinePath, String fragmentPath) {
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET, RenderPipelines.GUI_TEXTURED_SNIPPET).withLocation(Identifier.fromNamespaceAndPath(FascinatedUtils.MOD_ID, pipelinePath)).withVertexShader(Identifier.withDefaultNamespace("core/position_tex_color")).withFragmentShader(Identifier.fromNamespaceAndPath(FascinatedUtils.MOD_ID, fragmentPath)).withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false)).withCull(false).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).withSampler("Sampler0").build());
    }

    /**
     * Preset rounded pipeline for {@link RectCornerRoundMask#ALL}, {@link RectCornerRoundMask#TOP},
     * {@link RectCornerRoundMask#BOTTOM}, {@link RectCornerRoundMask#LEFT}, or {@link RectCornerRoundMask#RIGHT}.
     *
     * @param cornerRoundMask bitmask passed to the UI renderer
     * @return one of the five preset pipelines; caller must only pass masks that match those constants
     */
    public static RenderPipeline roundedRectPresetPipeline(int cornerRoundMask) {
        if (cornerRoundMask == RectCornerRoundMask.ALL) {
            return ROUNDED_RECT_ALL;
        }
        if (cornerRoundMask == RectCornerRoundMask.TOP) {
            return ROUNDED_RECT_TOP;
        }
        if (cornerRoundMask == RectCornerRoundMask.BOTTOM) {
            return ROUNDED_RECT_BOTTOM;
        }
        if (cornerRoundMask == RectCornerRoundMask.LEFT) {
            return ROUNDED_RECT_LEFT;
        }
        if (cornerRoundMask == RectCornerRoundMask.RIGHT) {
            return ROUNDED_RECT_RIGHT;
        }
        throw new IllegalArgumentException("cornerRoundMask is not a preset rounded mask: " + cornerRoundMask);
    }

    /**
     * @return true if this pipeline reads per-corner radii from {@link RoundedRectCornerRadiiTexture} via
     * {@code Sampler1} (needs mesh splits / uploads coordinated with draws)
     */
    public static boolean usesCornerRadiiLut(RenderPipeline pipeline) {
        return pipeline == ROUNDED_RECT_TEX_LUT;
    }
}
