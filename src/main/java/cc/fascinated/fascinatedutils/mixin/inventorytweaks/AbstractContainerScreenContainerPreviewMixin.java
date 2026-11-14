package cc.fascinated.fascinatedutils.mixin.inventorytweaks;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.ContainerPreviewModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenContainerPreviewMixin {

    @Shadow
    protected Slot hoveredSlot;

    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private void alumite$replaceTooltipWithPreview(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        ModuleRegistry.INSTANCE.getModule(ContainerPreviewModule.class).ifPresent(module -> {
            if (module.renderPreview(graphics, hoveredSlot, mouseX, mouseY)) {
                ci.cancel();
            }
        });
    }
}
