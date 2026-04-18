package cc.fascinated.fascinatedutils.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyBindingAccessorMixin {

    /**
     * Read the currently bound key for this mapping (same field vanilla reads in {@code KeyBinding.updatePressedStates}).
     */
    @Accessor("key")
    InputConstants.Key getKey();
}
