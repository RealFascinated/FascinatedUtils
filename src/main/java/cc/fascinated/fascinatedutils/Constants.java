package cc.fascinated.fascinatedutils;

import cc.fascinated.fascinatedutils.api.channel.json.ChannelDetailDTO;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelDetailDTODeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.NonNull;
import net.fabricmc.loader.api.FabricLoader;

public class Constants {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().registerTypeAdapter(ChannelDetailDTO.class, new ChannelDetailDTODeserializer()).create();

    @NonNull public static final String MOD_VERSION = FabricLoader.getInstance().getModContainer(AlumiteMod.MOD_ID).map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("?");
    @NonNull public static final String GAME_VERSION = FabricLoader.getInstance().getModContainer("minecraft").map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("?");

    public static final boolean DEBUG_MODE = FabricLoader.getInstance().isDevelopmentEnvironment();
}
