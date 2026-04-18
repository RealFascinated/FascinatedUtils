package cc.fascinated.fascinatedutils.systems.modules.impl.hypixel;

import cc.fascinated.fascinatedutils.common.PatternHandler;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.impl.hypixel.feature.AutoGG;
import lombok.Getter;
import net.minecraft.client.Minecraft;

@Getter
public class HypixelModule extends Module {

    private static final String HYPIXEL_BRANDING = "Hypixel BungeeCord \\(.+\\) <- .+";

    private final BooleanSetting autoGG = BooleanSetting.builder().id("auto_gg").defaultValue(false).build();

    public HypixelModule() {
        super("Hypixel");
        addSetting(autoGG);

        new AutoGG(this);
    }

    public boolean isOnHypixel() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        String serverBrand = minecraft.player.connection.serverBrand();
        if (serverBrand == null) {
            return false;
        }
        return PatternHandler.INSTANCE.getPattern(HYPIXEL_BRANDING).matcher(serverBrand).matches();
    }
}
