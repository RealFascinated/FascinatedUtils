package cc.fascinated.fascinatedutils.renderer;

import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Batched 2D GUI mesh: records {@link GuiElementRenderState} quads for custom {@link FascinatedUiPipelines} and
 * flushes them into the active {@link GuiGraphicsExtractor} {@code GuiRenderState} (replaces the old {@code GpuQuadBatcher} path).
 */
public class MeshRenderer {
    public static final MeshRenderer INSTANCE = new MeshRenderer();
    private static final Field GUI_SCISSOR_STACK_FIELD = resolveScissorStackField();
    private static final Method GUI_SCISSOR_PEEK_METHOD = resolveScissorPeekMethod();
    private static final Identifier WHITE_BLOCK_TEXTURE = Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private final List<GuiElementRenderState> pendingSolid = new ArrayList<>();
    private final List<GuiElementRenderState> pendingTextured = new ArrayList<>();
    private final List<DynamicTexture> disposableCornerRadiiLuts = new ArrayList<>();
    private TextureSetup whiteTextureSetup;

    private MeshRenderer() {
    }

    /**
     * Pack ARGB so the alpha byte holds {@code min(255, round(cornerRadius))}; RGB comes from {@code argb}.
     *
     * @param argb         source color (alpha is replaced)
     * @param cornerRadius corner fillet radius in logical pixels
     * @return packed color for all four quad vertices (same alpha byte on each)
     */
    public static int packArgbRadius(int argb, float cornerRadius) {
        int radiusByte = Mth.clamp(Math.round(cornerRadius), 0, 255);
        return (argb & 0x00FFFFFF) | (radiusByte << 24);
    }

    private static Field resolveScissorStackField() {
        try {
            Field field = GuiGraphicsExtractor.class.getDeclaredField("scissorStack");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Method resolveScissorPeekMethod() {
        if (GUI_SCISSOR_STACK_FIELD == null) {
            return null;
        }
        try {
            Method method = GUI_SCISSOR_STACK_FIELD.getType().getDeclaredMethod("peek");
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    /**
     * Drain queued elements in renderer order (solid then textured).
     *
     * @param drawContext draw context for the active GUI pass
     */

    private static ScreenRectangle currentScissor(GuiGraphicsExtractor drawContext) {
        if (GUI_SCISSOR_STACK_FIELD == null || GUI_SCISSOR_PEEK_METHOD == null) {
            return null;
        }
        try {
            Object stack = GUI_SCISSOR_STACK_FIELD.get(drawContext);
            return (ScreenRectangle) GUI_SCISSOR_PEEK_METHOD.invoke(stack);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    /**
     * Start recording quads for the active scissor segment (no-op for this implementation).
     *
     * @param drawContext draw context for the active GUI pass
     */
    public void beginSegment(GuiGraphicsExtractor drawContext) {
    }

    /**
     * Submit solid queue then textured queue to {@link net.minecraft.client.renderer.state.gui.GuiRenderState}.
     *
     * @param drawContext draw context for the active GUI pass
     */
    public void endSegment(GuiGraphicsExtractor drawContext) {
        flush(drawContext);
    }

    /**
     * Call from a mixin at the tail of {@link net.minecraft.client.gui.render.GuiRenderer#render} so GPU sampling has
     * finished before {@link DynamicTexture#close()} invalidates views used by the pass.
     */
    public void releaseDisposableRadiiLutsAfterGuiRenderPass() {
        for (DynamicTexture texture : disposableCornerRadiiLuts) {
            texture.close();
        }
        disposableCornerRadiiLuts.clear();
    }

    public void flush(GuiGraphicsExtractor drawContext) {
        if (pendingSolid.isEmpty() && pendingTextured.isEmpty()) {
            return;
        }
        for (GuiElementRenderState element : pendingSolid) {
            drawContext.guiRenderState.addGuiElement(element);
        }
        pendingSolid.clear();
        for (GuiElementRenderState element : pendingTextured) {
            drawContext.guiRenderState.addGuiElement(element);
        }
        pendingTextured.clear();
    }

    /**
     * Queue an axis-aligned solid {@code pos_color} quad (four corner colors).
     */
    public void enqueueSolidQuad(GuiGraphicsExtractor drawContext, float positionX, float positionY, float width, float height, int colorTopLeft, int colorBottomLeft, int colorBottomRight, int colorTopRight) {
        Matrix3x2f pose = new Matrix3x2f(drawContext.pose());
        ScreenRectangle scissor = currentScissor(drawContext);
        int x0 = Mth.floor(positionX);
        int y0 = Mth.floor(positionY);
        int x1 = Mth.ceil(positionX + width);
        int y1 = Mth.ceil(positionY + height);
        pendingSolid.add(new SolidColorQuadRenderState(FascinatedUiPipelines.SOLID_COLOR, pose, x0, y0, x1, y1, colorTopLeft, colorBottomLeft, colorBottomRight, colorTopRight, scissor));
    }

    /**
     * Queue an axis-aligned textured quad (white texture) with four corner colors (tinted rect or vertical gradient).
     */
    public void enqueueAxisTexQuad(GuiGraphicsExtractor drawContext, float positionX, float positionY, float width, float height, int colorTopLeft, int colorBottomLeft, int colorBottomRight, int colorTopRight) {
        Matrix3x2f pose = new Matrix3x2f(drawContext.pose());
        ScreenRectangle scissor = currentScissor(drawContext);
        int x0 = Mth.floor(positionX);
        int y0 = Mth.floor(positionY);
        int x1 = Mth.ceil(positionX + width);
        int y1 = Mth.ceil(positionY + height);
        pendingTextured.add(new AxisTexColorQuadRenderState(FascinatedUiPipelines.AXIS_TEX_COLOR, whiteSetup(), pose, x0, y0, x1, y1, colorTopLeft, colorBottomLeft, colorBottomRight, colorTopRight, scissor));
    }

    /**
     * Queue a filled rounded rectangle (vertical gradient supported via top/bottom colors).
     */
    public void enqueueRoundedGradient(GuiGraphicsExtractor drawContext, float positionX, float positionY, float width, float height, float cornerRadius, int colorTop, int colorBottom, int cornerRoundMask) {
        enqueueRoundedGradient(drawContext, positionX, positionY, width, height, cornerRadius, colorTop, colorBottom, cornerRoundMask, 0f);
    }

    /**
     * Queues a rounded rectangle; when {@code lutOuterRingStrokePx} is positive, {@code fui_rounded_rect} draws only an
     * outer border band of that thickness (LUT texel {@code (4,0)}) and the preset opaque fast path is disabled so the
     * LUT shader always runs.
     */
    public void enqueueRoundedGradient(GuiGraphicsExtractor drawContext, float positionX, float positionY, float width, float height, float cornerRadius, int colorTop, int colorBottom, int cornerRoundMask, float lutOuterRingStrokePx) {
        if (width < 1e-3f || height < 1e-3f) {
            return;
        }
        if (cornerRoundMask == RectCornerRoundMask.NONE) {
            enqueueAxisTexQuad(drawContext, positionX, positionY, width, height, colorTop, colorBottom, colorBottom, colorTop);
            return;
        }
        Matrix3x2f pose = new Matrix3x2f(drawContext.pose());
        ScreenRectangle scissor = currentScissor(drawContext);
        int x0 = Mth.floor(positionX);
        int y0 = Mth.floor(positionY);
        int x1 = Mth.ceil(positionX + width);
        int y1 = Mth.ceil(positionY + height);
        // Float-square shapes (slider thumb) must stay square in pixel space; independent floor/ceil per axis can be 11×10.
        if (Math.abs(width - height) < 1e-3f) {
            int pixelWidth = x1 - x0;
            int pixelHeight = y1 - y0;
            if (pixelWidth != pixelHeight) {
                int side = Math.max(pixelWidth, pixelHeight);
                float centerX = positionX + width * 0.5f;
                float centerY = positionY + height * 0.5f;
                x0 = Mth.floor(centerX - side * 0.5f);
                x1 = x0 + side;
                y0 = Mth.floor(centerY - side * 0.5f);
                y1 = y0 + side;
            }
        }
        int topAlpha = (colorTop >>> 24) & 0xFF;
        int bottomAlpha = (colorBottom >>> 24) & 0xFF;
        boolean presetOpaque = lutOuterRingStrokePx <= 0f && RectCornerRoundMask.isPresetMask(cornerRoundMask) && topAlpha == 255 && bottomAlpha == 255;
        RenderPipeline roundedPipeline;
        TextureSetup roundedTextureSetup;
        int topLeft;
        int bottomLeft;
        int bottomRight;
        int topRight;
        int packedCornerRadii = RectCornerRoundMask.packedCornerRadiiBytes(cornerRadius, cornerRoundMask);
        if (presetOpaque) {
            roundedPipeline = FascinatedUiPipelines.roundedRectPresetPipeline(cornerRoundMask);
            roundedTextureSetup = whiteSetup();
            topLeft = packArgbRadius(colorTop, cornerRadius);
            bottomLeft = packArgbRadius(colorBottom, cornerRadius);
            bottomRight = packArgbRadius(colorBottom, cornerRadius);
            topRight = packArgbRadius(colorTop, cornerRadius);
        }
        else {
            roundedPipeline = FascinatedUiPipelines.ROUNDED_RECT_TEX_LUT;
            Minecraft client = Minecraft.getInstance();
            if (client == null) {
                return;
            }
            AbstractTexture whiteBlock = client.getTextureManager().getTexture(WHITE_BLOCK_TEXTURE);
            DynamicTexture cornerRadiiLut = RoundedRectCornerRadiiTexture.createDisposableRadiiLut(packedCornerRadii, lutOuterRingStrokePx);
            disposableCornerRadiiLuts.add(cornerRadiiLut);
            roundedTextureSetup = TextureSetup.doubleTexture(whiteBlock.getTextureView(), whiteBlock.getSampler(), cornerRadiiLut.getTextureView(), cornerRadiiLut.getSampler());
            topLeft = colorTop;
            bottomLeft = colorBottom;
            bottomRight = colorBottom;
            topRight = colorTop;
        }
        pendingTextured.add(new RoundedRectTexRenderState(roundedPipeline, roundedTextureSetup, pose, x0, y0, x1, y1, topLeft, bottomLeft, bottomRight, topRight, scissor, presetOpaque ? 0 : packedCornerRadii, presetOpaque ? 0f : lutOuterRingStrokePx));
    }

    private TextureSetup whiteSetup() {
        if (whiteTextureSetup != null) {
            return whiteTextureSetup;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return TextureSetup.noTexture();
        }
        AbstractTexture texture = client.getTextureManager().getTexture(WHITE_BLOCK_TEXTURE);
        whiteTextureSetup = TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler());
        return whiteTextureSetup;
    }

}
