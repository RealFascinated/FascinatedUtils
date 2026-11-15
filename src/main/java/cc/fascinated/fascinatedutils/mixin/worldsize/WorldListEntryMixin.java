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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.WorldSelectionList$WorldListEntry")
public class WorldListEntryMixin {

    @Unique
    private static final int ALUMITE$SECONDARY_COLOR = -8355712;

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
    private void alumite$prependSizeToInfoText(CallbackInfo ci) {
        WorldSizeModule module = ModuleRegistry.INSTANCE.getModule(WorldSizeModule.class).orElse(null);
        Component baseInfo = ComponentUtils.mergeStyles(this.summary.getInfo(), Style.EMPTY.withColor(ALUMITE$SECONDARY_COLOR));
        Component message = baseInfo;
        if (module != null && module.isEnabled()) {
            String size = module.getFormattedSize(this.summary.getLevelId()).orElse(null);
            if (size != null) {
                message = Component.empty().append(baseInfo).append(Component.literal(" (" + size + ")").withStyle(Style.EMPTY.withColor(ALUMITE$SECONDARY_COLOR)));
            }
        }
        this.infoText.setMessage(message);
        this.infoText.setMaxWidth(this.list.getRowWidth() - this.getTextX() - 2);
    }
}
