package cc.fascinated.fascinatedutils.caches;

import cc.fascinated.fascinatedutils.common.LRUCache;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public class ItemStackSizeCache {

    private static final LRUCache<Integer, Long> SIZE_CACHE = new LRUCache<>(5_000);

    public static long getStackSize(ItemStack stack) {
        return SIZE_CACHE.computeIfAbsent(ItemStack.hashItemAndComponents(stack), _ -> {
            Minecraft client = Minecraft.getInstance();
            if (client.getConnection() == null) {
                return 0L;
            }
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), client.getConnection().registryAccess());
            try {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
                return (long) buf.readableBytes();
            } finally {
                buf.release();
            }
        });
    }
}
