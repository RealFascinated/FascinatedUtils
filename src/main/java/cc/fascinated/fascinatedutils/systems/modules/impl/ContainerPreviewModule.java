package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.mixin.inventorytweaks.GuiGraphicsExtractorTooltipInvoker;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import cc.fascinated.fascinatedutils.systems.modules.impl.containerpreview.ClientContainerPreviewTooltip;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
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

    public ContainerPreviewModule() {
        super("Container Preview", ModuleCategory.GENERAL);
    }

    /**
     * Replaces the hovered container item's tooltip with a grid preview of its contents, drawn through the vanilla tooltip path.
     *
     * @param graphics the graphics extractor to draw with
     * @param hoveredSlot the slot under the cursor, may be null
     * @param mouseX the cursor x position
     * @param mouseY the cursor y position
     * @return true if the preview was queued and the vanilla tooltip should be skipped
     */
    public boolean renderPreview(GuiGraphicsExtractor graphics, Slot hoveredSlot, int mouseX, int mouseY) {
        if (!isEnabled() || hoveredSlot == null) {
            return false;
        }
        ItemStack stack = hoveredSlot.getItem();
        if (stack.isEmpty()) {
            return false;
        }
        List<ItemStack> contents = readContents(stack);
        if (contents == null || contents.stream().allMatch(ItemStack::isEmpty)) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        List<ClientTooltipComponent> lines = List.of(ClientTooltipComponent.create(stack.getStyledHoverName().getVisualOrderText()), new ClientContainerPreviewTooltip(contents));
        ((GuiGraphicsExtractorTooltipInvoker) graphics).alumite$setTooltipForNextFrame(minecraft.font, lines, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, stack.get(DataComponents.TOOLTIP_STYLE), true);
        return true;
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
