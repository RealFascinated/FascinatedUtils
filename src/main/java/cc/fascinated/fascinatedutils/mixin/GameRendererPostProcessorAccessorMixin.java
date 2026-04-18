package cc.fascinated.fascinatedutils.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererPostProcessorAccessorMixin {
    @Invoker("setPostEffect")
    void fascinatedutils$setPostProcessor(Identifier id);

    @Accessor("effectActive")
    void fascinatedutils$setPostProcessorEnabled(boolean enabled);
}
