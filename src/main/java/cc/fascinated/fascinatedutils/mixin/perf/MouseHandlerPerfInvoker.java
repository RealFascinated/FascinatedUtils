package cc.fascinated.fascinatedutils.mixin.perf;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Lets perf scenes feed real mouse input through the vanilla handler, so the mod's mouse mixins run as in play.
 */
@Mixin(MouseHandler.class)
public interface MouseHandlerPerfInvoker {
    @Accessor("accumulatedDX")
    void alumite$setAccumulatedDX(double accumulatedDX);

    @Accessor("accumulatedDY")
    void alumite$setAccumulatedDY(double accumulatedDY);

    @Invoker("turnPlayer")
    void alumite$turnPlayer(double movementTime);

    @Invoker("onScroll")
    void alumite$onScroll(long windowHandle, double scrollX, double scrollY);

    @Invoker("onButton")
    void alumite$onButton(long windowHandle, MouseButtonInfo buttonInfo, int action);
}
