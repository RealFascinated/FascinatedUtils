package cc.fascinated.fascinatedutils.mixin.worldsize;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.WorldSizeModule;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.WorldSelectionList$WorldListEntry")
public class WorldListEntryMixin {

    @Shadow
    private LevelSummary summary;

    @Shadow
    private StringWidget infoText;

    @Shadow
    private WorldSelectionList list;

    @Shadow
    private int getTextX() {
        throw new AssertionError();
    }

    @Inject(method = "extractContent", at = @At("HEAD"))
    private void fascinatedutils$prependSizeToInfoText(CallbackInfo ci) {
        WorldSizeModule module = ModuleRegistry.INSTANCE.getModule(WorldSizeModule.class).orElse(null);
        Component baseInfo = ComponentUtils.mergeStyles(this.summary.getInfo(), Style.EMPTY.withColor(-8355712));
        if (module == null || !module.isEnabled()) {
            this.infoText.setMessage(baseInfo);
            this.infoText.setMaxWidth(this.list.getRowWidth() - this.getTextX() - 2);
            return;
        }
        String size = module.getFormattedSize(this.summary.getLevelId()).orElse(null);
        if (size == null) {
            this.infoText.setMessage(baseInfo);
            this.infoText.setMaxWidth(this.list.getRowWidth() - this.getTextX() - 2);
            return;
        }
        Component combined = Component.empty()
                .append(baseInfo)
                .append(Component.literal(" (" + size + ")").withStyle(Style.EMPTY.withColor(-8355712)));
        this.infoText.setMessage(combined);
        this.infoText.setMaxWidth(this.list.getRowWidth() - this.getTextX() - 2);
    }
}
