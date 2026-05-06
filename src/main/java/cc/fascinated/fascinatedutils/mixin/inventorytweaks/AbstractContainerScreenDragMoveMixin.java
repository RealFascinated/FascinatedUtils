package cc.fascinated.fascinatedutils.mixin.inventorytweaks;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.InventoryTweaksModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.inventorytweaks.QuickMove;
import cc.fascinated.fascinatedutils.systems.modules.impl.inventorytweaks.ScrollMove;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;


@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenDragMoveMixin {

    @Unique
    private final Set<Integer> alumite$visitedSlots = new HashSet<>();
    @Shadow
    protected Slot hoveredSlot;
    @Final
    @Shadow
    protected AbstractContainerMenu menu;
    @Unique
    private boolean alumite$shiftDragging = false;
    @Unique
    private boolean alumite$didDrag = false;
    @Unique
    private double alumite$scrollAccumulator = 0;

    @Shadow
    protected abstract void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput);

    @Inject(method = "mouseClicked", at = @At("RETURN"))
    private void alumite$afterMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        Optional<QuickMove> quickMove = alumite$quickMove();
        if (!quickMove.map(QuickMove::isEnabled).orElse(false)) {
            alumite$abortQuickMoveDragSession();
            return;
        }
        if (event.button() != 0) {
            if (alumite$shiftDragging) {
                alumite$abortQuickMoveDragSession();
            }
            return;
        }
        if (!event.hasShiftDown()) {
            if (alumite$shiftDragging) {
                alumite$abortQuickMoveDragSession();
            }
            return;
        }
        boolean clickLeftRoomForDragOutsideSlots = hoveredSlot != null || !Boolean.TRUE.equals(cir.getReturnValue());
        if (!clickLeftRoomForDragOutsideSlots) {
            alumite$abortQuickMoveDragSession();
            return;
        }
        alumite$shiftDragging = true;
        alumite$didDrag = false;
        alumite$visitedSlots.clear();
        if (hoveredSlot != null) {
            int slotId = menu.slots.indexOf(hoveredSlot);
            if (slotId >= 0) {
                alumite$visitedSlots.add(slotId);
            }
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void alumite$onMouseDragged(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        Optional<QuickMove> featureOpt = alumite$quickMove();
        if (!alumite$shiftDragging || !featureOpt.map(QuickMove::isEnabled).orElse(false)) {
            return;
        }
        com.mojang.blaze3d.platform.Window window = Minecraft.getInstance().getWindow();
        boolean shiftHeld = event.hasShiftDown() || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (event.button() != 0 || !shiftHeld) {
            alumite$abortQuickMoveDragSession();
            return;
        }
        if (hoveredSlot == null) {
            return;
        }
        int slotId = menu.slots.indexOf(hoveredSlot);
        if (slotId >= 0) {
            boolean visited = featureOpt.map(feature -> feature.tryVisitSlot(hoveredSlot, slotId, alumite$visitedSlots, this::slotClicked)).orElse(false);
            if (visited) {
                alumite$didDrag = true;
            }
        }
        cir.setReturnValue(true);
        cir.cancel();
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void alumite$onMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0) {
            if (alumite$shiftDragging) {
                alumite$abortQuickMoveDragSession();
            }
            return;
        }
        boolean wasDragging = alumite$shiftDragging && alumite$didDrag;
        alumite$abortQuickMoveDragSession();
        if (wasDragging) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void alumite$onMouseScrolled(double x, double y, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        Optional<ScrollMove> featureOpt = alumite$scrollMove();
        if (!featureOpt.map(ScrollMove::isEnabled).orElse(false)) {
            return;
        }
        if ((Object) this instanceof CreativeModeInventoryScreen) {
            return;
        }
        if (hoveredSlot == null) {
            alumite$scrollAccumulator = 0;
            return;
        }
        if (!Minecraft.getInstance().player.containerMenu.getCarried().isEmpty()) {
            return;
        }

        alumite$scrollAccumulator += scrollY;
        int ticks = (int) alumite$scrollAccumulator;
        if (ticks == 0) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }
        alumite$scrollAccumulator -= ticks;

        featureOpt.ifPresent(feature -> feature.scroll(ticks, hoveredSlot, menu, this::slotClicked));

        cir.setReturnValue(true);
        cir.cancel();
    }

    @Unique
    private void alumite$abortQuickMoveDragSession() {
        alumite$shiftDragging = false;
        alumite$didDrag = false;
        alumite$visitedSlots.clear();
    }

    @Unique
    private Optional<QuickMove> alumite$quickMove() {
        return ModuleRegistry.INSTANCE.getModule(InventoryTweaksModule.class).map(InventoryTweaksModule::getQuickMoveFeature);
    }

    @Unique
    private Optional<ScrollMove> alumite$scrollMove() {
        return ModuleRegistry.INSTANCE.getModule(InventoryTweaksModule.class).map(InventoryTweaksModule::getScrollMoveFeature);
    }
}
