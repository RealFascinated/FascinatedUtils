package cc.fascinated.fascinatedutils.mixin.itemtooltip;

import cc.fascinated.fascinatedutils.caches.ItemStackSizeCache;
import cc.fascinated.fascinatedutils.common.ByteFormatterUtil;
import cc.fascinated.fascinatedutils.common.LRUCache;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.ItemTooltipModule;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public class ItemStackTooltipMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void fascinatedutils$appendItemTooltip(CallbackInfoReturnable<List<Component>> cir) {
        ItemTooltipModule module = ModuleRegistry.INSTANCE.getModule(ItemTooltipModule.class).orElse(null);
        if (module == null || !module.isEnabled()) {
            return;
        }

        ItemStack stack = (ItemStack) (Object) this;

        if (module.getShowItemSize().isEnabled()) {
            long byteSize = ItemStackSizeCache.getStackSize(stack);
            if (byteSize > 0) {
                List<Component> mutable = new ArrayList<>(cir.getReturnValue());
                if (module.getGapAboveInfo().isEnabled()) {
                    mutable.add(Component.literal(""));
                }
                mutable.add(Component.literal("Size: " + ByteFormatterUtil.formatBytes(byteSize, 2)).withStyle(style -> style.withColor(0xAAAAAA)));
                cir.setReturnValue(mutable);
            }
        }
    }
}
