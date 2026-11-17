package cc.fascinated.fascinatedutils.gui.node.display;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * A GUI item icon with the vanilla count and durability overlays. The node is as wide as the icon plus the room the
 * count text needs to the left and right of it, and 16 pixels tall; the icon itself is drawn with the current alpha.
 */
public class ItemStackNode extends PositionedNode<ItemStackNode> {
    /**
     * Edge length of an item icon in pixels.
     */
    public static final int ICON_SIZE = 16;
    private static final Map<Item, ItemStack> PREVIEW_STACKS = new HashMap<>();
    private static final Map<Item, ItemStack> ICON_STACKS = new HashMap<>();

    private final ItemStack stack;

    /**
     * Creates an icon node.
     *
     * @param stack the stack to draw; an empty stack draws nothing but keeps its size
     */
    public ItemStackNode(ItemStack stack) {
        this.stack = stack;
    }

    /**
     * Builds a stack that renders before a world is loaded, for editor previews. It is backed by
     * {@link Holder#direct(Object, DataComponentMap)} so it never touches registry components, which are only bound
     * once a world loads, and carries the item model and a durability bar so previews show an overlay.
     *
     * @param item the item to preview
     * @return a shared preview stack, or {@link ItemStack#EMPTY} when the item is not registered
     */
    public static ItemStack previewStack(Item item) {
        return PREVIEW_STACKS.computeIfAbsent(item, ItemStackNode::buildPreviewStack);
    }

    /**
     * Returns a shared single-item stack to use as an icon, so callers that show an item every frame do not allocate a
     * stack each time. The stack must only be drawn, never modified.
     *
     * @param item the item to show
     * @return a shared stack holding one of the item
     */
    public static ItemStack iconStack(Item item) {
        return ICON_STACKS.computeIfAbsent(item, ItemStack::new);
    }

    private static ItemStack buildPreviewStack(Item item) {
        ResourceKey<Item> key = BuiltInRegistries.ITEM.wrapAsHolder(item).unwrapKey().orElse(null);
        if (key == null) {
            return ItemStack.EMPTY;
        }
        Identifier modelId = key.identifier();
        DataComponentMap baseComponents = DataComponentMap.builder()
                .set(DataComponents.ITEM_MODEL, modelId)
                .set(DataComponents.MAX_DAMAGE, 100)
                .build();
        return new ItemStack(Holder.direct(item, baseComponents), 1, DataComponentPatch.EMPTY);
    }

    private boolean hasCountOverlay() {
        return !stack.isEmpty() && stack.getCount() != 1;
    }

    private int leftOverflow() {
        if (!hasCountOverlay()) {
            return 0;
        }
        return Math.max(0, Math.round(Minecraft.getInstance().font.width(String.valueOf(stack.getCount())) - 17f));
    }

    private int rightOverflow() {
        return hasCountOverlay() ? 1 : 0;
    }

    @Override
    protected int intrinsicWidth(AlumiteGuiRenderer renderFrame, int parentWidth) {
        return leftOverflow() + ICON_SIZE + rightOverflow();
    }

    @Override
    protected int intrinsicHeight(AlumiteGuiRenderer renderFrame, int parentHeight, int resolvedWidth) {
        return ICON_SIZE;
    }

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        float iconX = bounds().positionX() + leftOverflow();
        float iconY = bounds().positionY();
        Minecraft minecraftClient = Minecraft.getInstance();
        if (minecraftClient.player != null) {
            renderFrame.drawGuiItem(minecraftClient.player, stack, iconX, iconY);
        }
        else {
            renderFrame.drawGuiItem(stack, iconX, iconY);
        }
    }
}
