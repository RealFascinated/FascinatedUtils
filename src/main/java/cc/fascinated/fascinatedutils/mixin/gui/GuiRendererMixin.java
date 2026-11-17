package cc.fascinated.fascinatedutils.mixin.gui;

import cc.fascinated.fascinatedutils.debug.HudFrameProfiler;
import cc.fascinated.fascinatedutils.mixininterface.GuiItemRenderStateAlpha;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.vertex.BufferBuilder;
import java.nio.ByteBuffer;
import java.util.List;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.gui.render.GuiItemAtlas;
import org.apache.commons.lang3.mutable.MutableBoolean;
import net.minecraft.util.Mth;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Unique
    private boolean alumite$currentMeshHud;
    @Unique
    private int alumite$uploadIndex;
    @Unique
    private long alumite$elementStart;
    @Unique
    private long alumite$stateStart;
    @Unique
    private long alumite$meshStart;

    @Shadow
    private BufferBuilder bufferBuilder;
    @Shadow
    private RenderPipeline previousPipeline;
    @Shadow
    private TextureSetup previousTextureSetup;
    @Shadow
    private ScreenRectangle previousScissorArea;
    @Shadow
    private List<?> meshesToDraw;

    @Shadow
    protected abstract void recordMesh(BufferBuilder builder, RenderPipeline pipeline, TextureSetup textureSetup, ScreenRectangle scissorArea);

    /**
     * Replaces the hardcoded white tint on item atlas blits with the alpha captured at submit time, so item icons fade
     * with the rest of the GUI. The blit pipeline is premultiplied, so every channel is scaled by the alpha.
     *
     * @param color     the vanilla tint (always opaque white)
     * @param itemState the item being blitted
     * @return the premultiplied tint for the item's alpha
     */
    @ModifyArg(method = "submitBlitFromItemAtlas", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/BlitRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2f;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;Lnet/minecraft/client/gui/navigation/ScreenRectangle;)V"), index = 11)
    private int alumite$fadeItemAtlasBlit(int color, @Local(argsOnly = true) GuiItemRenderState itemState) {
        float alpha = ((GuiItemRenderStateAlpha) (Object) itemState).alumite$getAlpha();
        if (alpha >= 1f) {
            return color;
        }
        int channel = Mth.clamp(Mth.floor(alpha * 255f), 0, 255);
        return (channel << 24) | (channel << 16) | (channel << 8) | channel;
    }

    @Inject(method = "prepare", at = @At("HEAD"))
    private void alumite$beginHudPrepare(CallbackInfo callbackInfo) {
        HudFrameProfiler.INSTANCE.beginPrepare();
    }

    @Inject(method = "addElementsToMeshes", at = @At("HEAD"))
    private void alumite$resetMeshOwner(CallbackInfo callbackInfo) {
        alumite$currentMeshHud = false;
    }

    /**
     * While the HUD is profiled, keeps HUD elements and vanilla elements in separate meshes so the HUD's draws can be
     * told apart, and starts timing the work spent on a HUD element.
     *
     * @param elementState the element about to be turned into vertices
     * @param callbackInfo mixin callback
     */
    @Inject(method = "addElementToMesh", at = @At("HEAD"))
    private void alumite$beginHudElement(GuiElementRenderState elementState, CallbackInfo callbackInfo) {
        HudFrameProfiler profiler = HudFrameProfiler.INSTANCE;
        alumite$elementStart = 0L;
        if (!profiler.isMeasuring()) {
            return;
        }
        boolean hudElement = profiler.isHudElement(elementState);
        if (hudElement != alumite$currentMeshHud) {
            if (bufferBuilder != null) {
                recordMesh(bufferBuilder, previousPipeline, previousTextureSetup, previousScissorArea);
                bufferBuilder = null;
                previousPipeline = null;
                previousTextureSetup = null;
                previousScissorArea = null;
            }
            alumite$currentMeshHud = hudElement;
        }
        if (hudElement) {
            alumite$elementStart = System.nanoTime();
        }
    }

    @Inject(method = "addElementToMesh", at = @At("RETURN"))
    private void alumite$endHudElement(GuiElementRenderState elementState, CallbackInfo callbackInfo) {
        if (alumite$elementStart != 0L) {
            HudFrameProfiler.INSTANCE.addGuiPassNanos(System.nanoTime() - alumite$elementStart);
            alumite$elementStart = 0L;
        }
    }

    @Inject(method = "recordMesh", at = @At("HEAD"))
    private void alumite$beginMeshTiming(CallbackInfo callbackInfo) {
        // Skipped inside an element, whose own timing already covers the mesh it flushes.
        alumite$meshStart = HudFrameProfiler.INSTANCE.isMeasuring() && alumite$currentMeshHud && alumite$elementStart == 0L ? System.nanoTime() : 0L;
    }

    @Inject(method = "recordMesh", at = @At("RETURN"))
    private void alumite$endMeshTiming(CallbackInfo callbackInfo) {
        if (alumite$meshStart != 0L) {
            HudFrameProfiler.INSTANCE.addGuiPassNanos(System.nanoTime() - alumite$meshStart);
            alumite$meshStart = 0L;
        }
    }

    @Inject(method = "lambda$prepareText$0", at = @At("HEAD"))
    private void alumite$beginTextPrepare(GuiTextRenderState text, CallbackInfo callbackInfo) {
        alumite$stateStart = HudFrameProfiler.INSTANCE.beginStatePrepare(text);
    }

    @Inject(method = "lambda$prepareText$0", at = @At("RETURN"))
    private void alumite$endTextPrepare(GuiTextRenderState text, CallbackInfo callbackInfo) {
        HudFrameProfiler.INSTANCE.endStatePrepare(alumite$stateStart);
        alumite$stateStart = 0L;
    }

    @Inject(method = "lambda$prepareItemElements$0", at = @At("HEAD"))
    private void alumite$beginItemPrepare(MutableBoolean hasOversizedItems, GuiItemAtlas itemAtlas, GuiItemRenderState itemState, CallbackInfo callbackInfo) {
        alumite$stateStart = HudFrameProfiler.INSTANCE.beginStatePrepare(itemState);
    }

    @Inject(method = "lambda$prepareItemElements$0", at = @At("RETURN"))
    private void alumite$endItemPrepare(MutableBoolean hasOversizedItems, GuiItemAtlas itemAtlas, GuiItemRenderState itemState, CallbackInfo callbackInfo) {
        HudFrameProfiler.INSTANCE.endStatePrepare(alumite$stateStart);
        alumite$stateStart = 0L;
    }

    @Inject(method = "lambda$prepareItemElements$1", at = @At("HEAD"))
    private void alumite$beginOversizedItemPrepare(int guiScale, GuiItemRenderState itemState, CallbackInfo callbackInfo) {
        alumite$stateStart = HudFrameProfiler.INSTANCE.beginStatePrepare(itemState);
    }

    @Inject(method = "lambda$prepareItemElements$1", at = @At("RETURN"))
    private void alumite$endOversizedItemPrepare(int guiScale, GuiItemRenderState itemState, CallbackInfo callbackInfo) {
        HudFrameProfiler.INSTANCE.endStatePrepare(alumite$stateStart);
        alumite$stateStart = 0L;
    }

    @Inject(method = "lambda$preparePictureInPicture$0", at = @At("HEAD"))
    private void alumite$beginPipPrepare(int guiScale, PictureInPictureRenderState pipState, CallbackInfo callbackInfo) {
        alumite$stateStart = HudFrameProfiler.INSTANCE.beginStatePrepare(pipState);
    }

    @Inject(method = "lambda$preparePictureInPicture$0", at = @At("RETURN"))
    private void alumite$endPipPrepare(int guiScale, PictureInPictureRenderState pipState, CallbackInfo callbackInfo) {
        HudFrameProfiler.INSTANCE.endStatePrepare(alumite$stateStart);
        alumite$stateStart = 0L;
    }

    @Inject(method = "recordMesh", at = @At("RETURN"))
    private void alumite$noteMeshOwner(CallbackInfo callbackInfo) {
        HudFrameProfiler profiler = HudFrameProfiler.INSTANCE;
        if (profiler.isMeasuring()) {
            profiler.onMeshRecorded(meshesToDraw.size(), alumite$currentMeshHud);
        }
    }

    @Inject(method = "recordDraws", at = @At("HEAD"))
    private void alumite$beginUploads(CallbackInfo callbackInfo) {
        alumite$uploadIndex = 0;
    }

    @Inject(method = "recordDraws", at = @At("RETURN"))
    private void alumite$endUploads(CallbackInfo callbackInfo) {
        if (HudFrameProfiler.INSTANCE.isMeasuring()) {
            HudFrameProfiler.INSTANCE.onDrawsRecorded();
        }
    }

    @WrapOperation(method = "recordDraws", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;mapBuffer(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;ZZ)Lcom/mojang/blaze3d/buffers/GpuBuffer$MappedView;"))
    private GpuBuffer.MappedView alumite$timeHudBufferMap(CommandEncoder commandEncoder, GpuBufferSlice slice, boolean read, boolean write, Operation<GpuBuffer.MappedView> original) {
        HudFrameProfiler profiler = HudFrameProfiler.INSTANCE;
        int meshIndex = alumite$uploadIndex++;
        if (!profiler.isMeasuring() || !profiler.isHudMesh(meshIndex)) {
            return original.call(commandEncoder, slice, read, write);
        }
        long start = System.nanoTime();
        GpuBuffer.MappedView view = original.call(commandEncoder, slice, read, write);
        profiler.addGuiPassNanos(System.nanoTime() - start);
        return view;
    }

    @WrapOperation(method = "recordDraws", at = @At(value = "INVOKE", target = "Lorg/lwjgl/system/MemoryUtil;memCopy(Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)V"))
    private void alumite$timeHudUpload(ByteBuffer source, ByteBuffer target, Operation<Void> original) {
        HudFrameProfiler profiler = HudFrameProfiler.INSTANCE;
        if (!profiler.isMeasuring() || !profiler.isHudMesh(alumite$uploadIndex - 1)) {
            original.call(source, target);
            return;
        }
        long start = System.nanoTime();
        original.call(source, target);
        profiler.addGuiPassNanos(System.nanoTime() - start);
    }

    @Inject(method = "executeDrawRange", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDraw(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V"))
    private void alumite$beforeDraw(CallbackInfo callbackInfo, @Local(ordinal = 2) int drawIndex) {
        if (HudFrameProfiler.INSTANCE.isMeasuring()) {
            HudFrameProfiler.INSTANCE.onDrawStart(drawIndex);
        }
    }

    @Inject(method = "executeDrawRange", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDraw(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V", shift = At.Shift.AFTER))
    private void alumite$afterDraw(CallbackInfo callbackInfo, @Local(ordinal = 2) int drawIndex) {
        if (HudFrameProfiler.INSTANCE.isMeasuring()) {
            HudFrameProfiler.INSTANCE.onDrawEnd(drawIndex);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void alumite$endHudPass(CallbackInfo callbackInfo) {
        HudFrameProfiler.INSTANCE.endGuiPass();
    }
}
