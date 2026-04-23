package cc.fascinated.fascinatedutils.mixin.itemtooltip;

import cc.fascinated.fascinatedutils.common.ByteFormatterUtil;
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

import java.util.List;

@Mixin(ItemStack.class)
public class ItemStackTooltipMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void fascinatedutils$appendItemTooltip(CallbackInfoReturnable<List<Component>> cir) {
        ItemTooltipModule module = ModuleRegistry.INSTANCE.getModule(ItemTooltipModule.class).orElse(null);
        if (module == null || !module.isEnabled()) {
            return;
        }

        ItemStack stack = (ItemStack) (Object) this;

        if (module.getShowItemSize().isEnabled()) {
            long byteSize = serializeStackSize(stack);
            if (byteSize > 0) {
                String formatted = ByteFormatterUtil.formatBytes(byteSize, 2);

                cir.getReturnValue().add(Component.literal(""));
                cir.getReturnValue().add(Component.literal("Size: " + formatted).withStyle(style -> style.withColor(0xAAAAAA)));
            }
        }
    }

    @Unique
    private static long serializeStackSize(ItemStack stack) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) {
            return 0;
        }
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), client.getConnection().registryAccess()
        );
        try {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
            return buf.readableBytes();
        } finally {
            buf.release();
        }
    }
}
