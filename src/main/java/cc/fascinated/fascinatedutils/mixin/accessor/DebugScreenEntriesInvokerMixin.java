package cc.fascinated.fascinatedutils.mixin.accessor;

import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DebugScreenEntries.class)
public interface DebugScreenEntriesInvokerMixin {

    /**
     * Registers a debug screen entry so it appears in the F3 overlay and the debug options screen.
     *
     * @param identifier entry id
     * @param entry      entry to register
     * @return the registered id
     */
    @Invoker("register")
    static Identifier invokeRegister(Identifier identifier, DebugScreenEntry entry) {
        throw new AssertionError();
    }
}
