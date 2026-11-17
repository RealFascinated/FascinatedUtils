package cc.fascinated.fascinatedutils.mixin.gui;

import cc.fascinated.fascinatedutils.mixininterface.GuiItemRenderStateAlpha;
import cc.fascinated.fascinatedutils.renderer.Renderer2D;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Captures the alpha multiplier in effect when an item render state is constructed.
 *
 * <p>{@link Renderer2D} sets {@link Renderer2D#guiItemAlpha()} strictly around the vanilla item submission and resets
 * it afterwards, so only states created by that submission fade; every other item keeps full alpha. The captured value
 * is applied to the item atlas blit by {@code GuiRendererMixin}.
 */
@Mixin(GuiItemRenderState.class)
public abstract class GuiItemRenderStateMixin implements GuiItemRenderStateAlpha {

    @Unique
    private final float alumite$alpha = Renderer2D.guiItemAlpha();

    @Override
    public float alumite$getAlpha() {
        return alumite$alpha;
    }
}
