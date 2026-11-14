package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ContainerPreviewModule extends Module {
    private static final int SLOT_SIZE = 18;
    private static final int PADDING = 4;
    private static final int COLUMNS = 9;
    private static final int BACKGROUND_COLOR = 0xF0100010;
    private static final int BORDER_COLOR = 0xFF5000A0;
    private static final int SLOT_COLOR = 0x80000000;

    public ContainerPreviewModule() {
        super("Container Preview", ModuleCategory.GENERAL);
    }

    /**
     * Draws a grid preview of the hovered container item on hover.
     *
     * @param graphics the graphics extractor to draw with
     * @param hoveredSlot the slot under the cursor, may be null
     * @param mouseX the cursor x position
     * @param mouseY the cursor y position
     */
    public void renderPreview(GuiGraphicsExtractor graphics, Slot hoveredSlot, int mouseX, int mouseY) {
        if (!isEnabled() || hoveredSlot == null) {
            return;
        }
        ItemStack stack = hoveredSlot.getItem();
        if (stack.isEmpty()) {
            return;
        }
        List<ItemStack> contents = readContents(stack);
        if (contents == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int columns = Math.min(COLUMNS, contents.size());
        int rows = (contents.size() + COLUMNS - 1) / COLUMNS;
        int width = columns * SLOT_SIZE + PADDING * 2;
        int height = rows * SLOT_SIZE + PADDING * 2;
        int left = Math.min(mouseX + 12, graphics.guiWidth() - width - 2);
        int top = Math.min(mouseY - height - 4, graphics.guiHeight() - height - 2);
        left = Math.max(2, left);
        top = Math.max(2, top);

        graphics.nextStratum();
        graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, BORDER_COLOR);
        graphics.fill(left, top, left + width, top + height, BACKGROUND_COLOR);
        for (int index = 0; index < contents.size(); index++) {
            int slotX = left + PADDING + (index % COLUMNS) * SLOT_SIZE;
            int slotY = top + PADDING + (index / COLUMNS) * SLOT_SIZE;
            graphics.fill(slotX, slotY, slotX + SLOT_SIZE - 2, slotY + SLOT_SIZE - 2, SLOT_COLOR);
            ItemStack content = contents.get(index);
            if (!content.isEmpty()) {
                graphics.item(content, slotX, slotY);
                graphics.itemDecorations(font, content, slotX, slotY);
            }
        }
    }

    private List<ItemStack> readContents(ItemStack stack) {
        if (stack.is(Items.ENDER_CHEST)) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return null;
            }
            List<ItemStack> items = new ArrayList<>();
            for (int slot = 0; slot < minecraft.player.getEnderChestInventory().getContainerSize(); slot++) {
                items.add(minecraft.player.getEnderChestInventory().getItem(slot));
            }
            return items;
        }
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container != null) {
            NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
            container.copyInto(items);
            return items;
        }
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null && !bundle.isEmpty()) {
            return bundle.itemCopyStream().toList();
        }
        return null;
    }
}
