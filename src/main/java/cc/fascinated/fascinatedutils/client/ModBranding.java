package cc.fascinated.fascinatedutils.client;

import cc.fascinated.fascinatedutils.Constants;
import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;

@UtilityClass
public class ModBranding {
    /**
     * Title for the mod settings screen, including the loaded mod version from Fabric metadata.
     *
     * @return the translatable title component for the shell title bar
     */
    public Component modSettingsScreenTitle() {
        return Component.translatable("alumite.setting.shell.title", Constants.MOD_VERSION);
    }
}
