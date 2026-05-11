package cc.fascinated.fascinatedutils;

import cc.fascinated.fascinatedutils.api.channel.json.ChannelDetailDTO;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelDetailDTODeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.NonNull;
import net.fabricmc.loader.api.FabricLoader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Constants {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().registerTypeAdapter(ChannelDetailDTO.class, new ChannelDetailDTODeserializer()).create();

    @NonNull public static final String MOD_VERSION = FabricLoader.getInstance().getModContainer(AlumiteMod.MOD_ID).map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("?");
    @NonNull public static final String GAME_VERSION = FabricLoader.getInstance().getModContainer("minecraft").map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("?");

    public static final boolean DEBUG_MODE = FabricLoader.getInstance().isDevelopmentEnvironment();

    public static final ExecutorService EXECUTORS = Executors.newFixedThreadPool(4);
    public static final ScheduledExecutorService SCHEDULED_POOL = Executors.newScheduledThreadPool(4);
}
