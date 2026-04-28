package cc.fascinated.fascinatedutils.common;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import net.fabricmc.loader.api.FabricLoader;

public class AlumiteEnvironment {

    public static final String API_BASE_URL = System.getenv().getOrDefault("ALUMITE_API_URL", "https://alumite-api.fascinated.cc");
    public static final String USER_AGENT = "FascinatedUtils/" + FabricLoader.getInstance()
            .getModContainer(FascinatedUtils.MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
}
