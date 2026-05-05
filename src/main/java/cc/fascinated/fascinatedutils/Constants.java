package cc.fascinated.fascinatedutils;

import cc.fascinated.fascinatedutils.api.channel.json.ChannelDetailDTO;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelDetailDTODeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Constants {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().registerTypeAdapter(ChannelDetailDTO.class, new ChannelDetailDTODeserializer()).create();
}
