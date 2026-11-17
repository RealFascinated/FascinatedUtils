package cc.fascinated.fascinatedutils.common.item;

import cc.fascinated.fascinatedutils.common.collection.SynchronizedLRUCache;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public class ItemStackSizeCalculator {
    public static final ItemStackSizeCalculator INSTANCE = new ItemStackSizeCalculator();

    private final SynchronizedLRUCache<Integer, Long> sizeCache = new SynchronizedLRUCache<>(5_000);

    /**
     * Computes the network-encoded byte size of a stack.
     *
     * @param stack the stack to measure
     * @return the encoded size in bytes, or 0 when there is no connection to encode with
     */
    public long getStackSize(ItemStack stack) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) {
            // Not cached: a size computed without a connection would otherwise stick as 0 for this stack.
            return 0L;
        }
        return sizeCache.computeIfAbsent(ItemStack.hashItemAndComponents(stack), _ -> {
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
