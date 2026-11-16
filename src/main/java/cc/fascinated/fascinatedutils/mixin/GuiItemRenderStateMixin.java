package cc.fascinated.fascinatedutils.mixin;

import cc.fascinated.fascinatedutils.mixininterface.GuiItemRenderStateAlpha;
import cc.fascinated.fascinatedutils.renderer.Renderer2D;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiItemRenderState.class)
public abstract class GuiItemRenderStateMixin implements GuiItemRenderStateAlpha {

    @Unique
    private final float alumite$alpha = Renderer2D.guiItemAlpha();

    @Override
    public float alumite$getAlpha() {
        return alumite$alpha;
    }
}
