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

    @Shadow protected Slot hoveredSlot;
    @Final
    @Shadow protected AbstractContainerMenu menu;

    @Shadow
    protected abstract void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput);

    @Unique
    private boolean fascinatedutils$shiftDragging = false;
    @Unique
    private boolean fascinatedutils$didDrag = false;
    @Unique
    private final Set<Integer> fascinatedutils$visitedSlots = new HashSet<>();
    @Unique
    private double fascinatedutils$scrollAccumulator = 0;

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void fascinatedutils$onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!fascinatedutils$quickMove().map(QuickMove::isEnabled).orElse(false)) {
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
    private void fascinatedutils$onMouseDragged(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        Optional<QuickMove> featureOpt = fascinatedutils$quickMove();
        if (!fascinatedutils$shiftDragging || !featureOpt.map(QuickMove::isEnabled).orElse(false)) {
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
        if (slotId >= 0) {
            Slot slot = hoveredSlot;
            boolean visited = featureOpt.map(feature -> feature.tryVisitSlot(
                    slot, slotId, fascinatedutils$visitedSlots, this::slotClicked)).orElse(false);
            if (visited) {
                fascinatedutils$didDrag = true;
            }
        }
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
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$onMouseScrolled(double x, double y, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        Optional<ScrollMove> featureOpt = fascinatedutils$scrollMove();
        if (!featureOpt.map(ScrollMove::isEnabled).orElse(false)) {
            return;
        }
        if ((Object) this instanceof CreativeModeInventoryScreen) {
            return;
        }
        if (hoveredSlot == null) {
            fascinatedutils$scrollAccumulator = 0;
            return;
        }
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

        Slot slot = hoveredSlot;
        featureOpt.ifPresent(feature -> feature.scroll(
                ticks, slot, menu, this::slotClicked));

        cir.setReturnValue(true);
        cir.cancel();
    }

    @Unique
    private Optional<QuickMove> fascinatedutils$quickMove() {
        return ModuleRegistry.INSTANCE.getModule(InventoryTweaksModule.class).map(InventoryTweaksModule::getQuickMoveFeature);
    }

    @Unique
    private Optional<ScrollMove> fascinatedutils$scrollMove() {
        return ModuleRegistry.INSTANCE.getModule(InventoryTweaksModule.class).map(InventoryTweaksModule::getScrollMoveFeature);
    }
}

