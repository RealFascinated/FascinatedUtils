package cc.fascinated.fascinatedutils.client;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

@UtilityClass
public class ModBranding {
    /**
     * Title for the mod settings screen, including the loaded mod version from Fabric metadata.
     *
     * @return the translatable title component for the shell title bar
     */
    public Component modSettingsScreenTitle() {
        String version = FabricLoader.getInstance().getModContainer(FascinatedUtils.MOD_ID).map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("?");
        return Component.translatable("fascinatedutils.setting.shell.title", version);
    }
}
