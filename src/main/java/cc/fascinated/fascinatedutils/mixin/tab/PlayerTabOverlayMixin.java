package cc.fascinated.fascinatedutils.mixin.tab;

import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.TabModule;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.Optional;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {

    @ModifyConstant(constant = @Constant(longValue = 80L), method = "getPlayerInfos")
    private long modifyCount(long count) {
        Optional<TabModule> optionalTabModule = ModuleRegistry.INSTANCE.getModule(TabModule.class);
        if (optionalTabModule.isPresent()) {
            TabModule tabModule = optionalTabModule.get();
            if (tabModule.isEnabled()) {
                return count;
            }
            SliderSetting maxPlayerSlots = tabModule.getMaxPlayerSlots();
            return maxPlayerSlots.isDefault() ? count : maxPlayerSlots.getValue().longValue();
        }
        return count;
    }
}