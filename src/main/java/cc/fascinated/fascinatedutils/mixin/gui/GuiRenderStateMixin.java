package cc.fascinated.fascinatedutils.mixin.gui;

import cc.fascinated.fascinatedutils.debug.HudFrameProfiler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin {

    // Lets the HUD profiler attribute every state the HUD submits, and every glyph and blit vanilla derives from them.
    @Inject(method = "addGuiElement", at = @At("HEAD"))
    private void alumite$trackElement(GuiElementRenderState state, CallbackInfo callbackInfo) {
        HudFrameProfiler.INSTANCE.trackState(state);
    }

    @Inject(method = "addText", at = @At("HEAD"))
    private void alumite$trackText(GuiTextRenderState state, CallbackInfo callbackInfo) {
        HudFrameProfiler.INSTANCE.trackState(state);
    }

    @Inject(method = "addItem", at = @At("HEAD"))
    private void alumite$trackItem(GuiItemRenderState state, CallbackInfo callbackInfo) {
        HudFrameProfiler.INSTANCE.trackState(state);
    }

    @Inject(method = "addPicturesInPictureState", at = @At("HEAD"))
    private void alumite$trackPictureInPicture(PictureInPictureRenderState state, CallbackInfo callbackInfo) {
        HudFrameProfiler.INSTANCE.trackState(state);
    }

    @Inject(method = "addGlyphToCurrentLayer", at = @At("HEAD"))
    private void alumite$trackGlyph(GuiElementRenderState state, CallbackInfo callbackInfo) {
        HudFrameProfiler.INSTANCE.trackState(state);
    }

    @Inject(method = "addBlitToCurrentLayer", at = @At("HEAD"))
    private void alumite$trackBlit(BlitRenderState state, CallbackInfo callbackInfo) {
        HudFrameProfiler.INSTANCE.trackState(state);
    }

    /**
     * Times the sort of a node's elements when the node belongs to the HUD. A node holds only the HUD's elements or
     * only vanilla's, because the HUD owns its strata.
     *
     * @param elements   the node's elements
     * @param comparator vanilla's element comparator
     * @param original   the vanilla sort
     */
    @WrapOperation(method = "lambda$sortElements$0", at = @At(value = "INVOKE", target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"))
    private static void alumite$timeHudSort(List<GuiElementRenderState> elements, Comparator<GuiElementRenderState> comparator, Operation<Void> original) {
        HudFrameProfiler profiler = HudFrameProfiler.INSTANCE;
        if (!profiler.isMeasuring() || elements.isEmpty() || !profiler.isHudElement(elements.getFirst())) {
            original.call(elements, comparator);
            return;
        }
        long start = System.nanoTime();
        original.call(elements, comparator);
        profiler.addGuiPassNanos(System.nanoTime() - start);
    }
}
