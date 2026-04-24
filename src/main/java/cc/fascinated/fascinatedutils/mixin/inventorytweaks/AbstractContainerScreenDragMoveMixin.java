package cc.fascinated.fascinatedutils.mixin.inventorytweaks;

import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.InventoryTweaksModule;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;


@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenDragMoveMixin {

    @Shadow protected Slot hoveredSlot;
    @Shadow protected AbstractContainerMenu menu;

    @Shadow
    protected abstract void slotClicked(Slot slot, int slotId, int mouseButton, ContainerInput actionType);

    private boolean fascinatedutils$shiftDragging = false;
    private boolean fascinatedutils$didDrag = false;
    private final Set<Integer> fascinatedutils$visitedSlots = new HashSet<>();
    private double fascinatedutils$scrollAccumulator = 0;

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void fascinatedutils$onMouseClicked(MouseButtonEvent event, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (!isDragMoveEnabled()) {
            return;
        }
        if (event.button() == 0 && event.hasShiftDown() && hoveredSlot != null) {
            fascinatedutils$shiftDragging = true;
            fascinatedutils$didDrag = false;
            fascinatedutils$visitedSlots.clear();
            int slotId = menu.slots.indexOf(hoveredSlot);
            if (slotId >= 0) {
                fascinatedutils$visitedSlots.add(slotId);
            }
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$onMouseDragged(MouseButtonEvent event, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
        if (!fascinatedutils$shiftDragging || !isDragMoveEnabled()) {
            return;
        }
        com.mojang.blaze3d.platform.Window window = Minecraft.getInstance().getWindow();
        boolean shiftDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (event.button() != 0 || !shiftDown) {
            fascinatedutils$shiftDragging = false;
            fascinatedutils$visitedSlots.clear();
            return;
        }
        if (hoveredSlot == null) {
            return;
        }
        int slotId = menu.slots.indexOf(hoveredSlot);
        if (slotId >= 0 && fascinatedutils$visitedSlots.add(slotId)) {
            slotClicked(hoveredSlot, slotId, 0, ContainerInput.QUICK_MOVE);
        }
        fascinatedutils$didDrag = true;
        cir.setReturnValue(true);
        cir.cancel();
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$onMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() == 0) {
            boolean wasDragging = fascinatedutils$shiftDragging && fascinatedutils$didDrag;
            fascinatedutils$shiftDragging = false;
            fascinatedutils$didDrag = false;
            fascinatedutils$visitedSlots.clear();
            if (wasDragging) {
                // Prevent vanilla from performing a pickup click on the hovered slot
                // after our drag ends, which would leave an item stuck on the cursor.
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$onMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        if (!isScrollMoveEnabled()) {
            return;
        }
        if ((Object) this instanceof CreativeModeInventoryScreen) {
            return;
        }
        if (hoveredSlot == null) {
            fascinatedutils$scrollAccumulator = 0;
            return;
        }
        // Don't interfere if the player is already holding something on the cursor
        if (!Minecraft.getInstance().player.containerMenu.getCarried().isEmpty()) {
            return;
        }

        fascinatedutils$scrollAccumulator += scrollY;
        int ticks = (int) fascinatedutils$scrollAccumulator;
        if (ticks == 0) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }
        fascinatedutils$scrollAccumulator -= ticks;

        boolean push = ticks < 0; // scroll down = push item from hovered to other inventory
        int count = Math.abs(ticks);

        for (int i = 0; i < count; i++) {
            if (push) {
                if (!hoveredSlot.hasItem()) break;
                fascinatedutils$moveOne(hoveredSlot, true);
            } else {
                if (!hoveredSlot.hasItem()) break;
                boolean hoveredInPlayerInv = hoveredSlot.container == Minecraft.getInstance().player.getInventory();
                Slot source = fascinatedutils$findMatchingSource(!hoveredInPlayerInv, hoveredSlot.getItem());
                if (source == null) break;
                fascinatedutils$moveOne(source, true);
            }
        }

        cir.setReturnValue(true);
        cir.cancel();
    }

    /**
     * Picks up the full stack from {@code source}, drops exactly 1 item into the best available
     * target slot on the other side, then returns any remainder to {@code source}.
     *
     * @param source      the slot to take an item from
     * @param toOtherSide if true, move to the inventory opposite to source; otherwise move to
     *                    the same side as hoveredSlot
     */
    private void fascinatedutils$moveOne(Slot source, boolean toOtherSide) {
        ItemStack sourceStack = source.getItem();
        if (sourceStack.isEmpty()) return;

        boolean sourceInPlayerInv = source.container == Minecraft.getInstance().player.getInventory();
        boolean targetInPlayerInv = toOtherSide != sourceInPlayerInv;

        Slot target = fascinatedutils$findTarget(targetInPlayerInv, sourceStack);
        if (target == null) return;

        int sourceId = menu.slots.indexOf(source);
        int targetId = menu.slots.indexOf(target);

        slotClicked(source, sourceId, 0, ContainerInput.PICKUP);       // pick up all
        slotClicked(target, targetId, 1, ContainerInput.PICKUP);        // drop 1 (right-click)
        if (!Minecraft.getInstance().player.containerMenu.getCarried().isEmpty()) {
            slotClicked(source, sourceId, 0, ContainerInput.PICKUP);   // return remainder
        }
    }

    /**
     * Finds the best target slot on the given inventory side: prefers compatible non-full stacks,
     * then falls back to any empty slot that accepts the item.
     */
    private Slot fascinatedutils$findTarget(boolean inPlayerInv, ItemStack item) {
        Slot emptyFallback = null;
        for (Slot slot : menu.slots) {
            boolean slotInPlayerInv = slot.container == Minecraft.getInstance().player.getInventory();
            if (slotInPlayerInv != inPlayerInv) continue;
            if (slot.hasItem()) {
                ItemStack existing = slot.getItem();
                if (ItemStack.isSameItemSameComponents(existing, item)
                        && existing.getCount() < slot.getMaxStackSize(existing)) {
                    return slot; // compatible, not full — best choice
                }
            } else if (emptyFallback == null && slot.mayPlace(item)) {
                emptyFallback = slot;
            }
        }
        return emptyFallback;
    }

    /** Finds the first non-empty slot on the given inventory side whose item matches {@code filter}. */
    private Slot fascinatedutils$findMatchingSource(boolean inPlayerInv, ItemStack filter) {
        for (Slot slot : menu.slots) {
            boolean slotInPlayerInv = slot.container == Minecraft.getInstance().player.getInventory();
            if (slotInPlayerInv != inPlayerInv) continue;
            if (slot.hasItem() && ItemStack.isSameItemSameComponents(slot.getItem(), filter)) {
                return slot;
            }
        }
        return null;
    }

    private boolean isDragMoveEnabled() {
        return ModuleRegistry.INSTANCE.getModule(InventoryTweaksModule.class)
                .filter(Module::isEnabled)
                .map(module -> module.getQuickMove().isEnabled())
                .orElse(false);
    }

    private boolean isScrollMoveEnabled() {
        return ModuleRegistry.INSTANCE.getModule(InventoryTweaksModule.class)
                .filter(Module::isEnabled)
                .map(module -> module.getScrollMove().isEnabled())
                .orElse(false);
    }
}
