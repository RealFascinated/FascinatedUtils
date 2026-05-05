package cc.fascinated.fascinatedutils;

import cc.fascinated.fascinatedutils.api.channel.json.ChannelDetailWire;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelDetailWireDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Constants {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().registerTypeAdapter(ChannelDetailWire.class, new ChannelDetailWireDeserializer()).create();
}
