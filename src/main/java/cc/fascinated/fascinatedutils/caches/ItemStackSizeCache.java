package cc.fascinated.fascinatedutils.caches;

import cc.fascinated.fascinatedutils.common.LRUCache;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public class ItemStackSizeCache {

    private static final LRUCache<Integer, Long> SIZE_CACHE = new LRUCache<>(5_000);

    public static long getStackSize(ItemStack stack) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) {
            // Not cached: a size computed without a connection would otherwise stick as 0 for this stack.
            return 0L;
        }
        return SIZE_CACHE.computeIfAbsent(ItemStack.hashItemAndComponents(stack), _ -> {
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
