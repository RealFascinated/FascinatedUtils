package cc.fascinated.fascinatedutils.api.channel.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class ChannelDetailWireDeserializer implements JsonDeserializer<ChannelDetailWire> {

    @Override
    public ChannelDetailWire deserialize(JsonElement jsonElement, Type targetType, JsonDeserializationContext context) throws JsonParseException {
        if (jsonElement == null || !jsonElement.isJsonObject()) {
            throw new JsonParseException("Channel detail must be a JSON object.");
        }

        JsonObject root = jsonElement.getAsJsonObject();
        JsonElement typeElement = root.get("type");
        if (typeElement == null) {
            throw new JsonParseException("Channel detail is missing a type discriminator.");
        }

        ChannelKindWire channelType = context.deserialize(typeElement, ChannelKindWire.class);
        if (channelType == null) {
            throw new JsonParseException("Channel detail has an unknown type discriminator.");
        }

        return switch (channelType) {
            case DM -> context.deserialize(root, ChannelDetailWire.DmChannelDetailWire.class);
            case GROUP -> context.deserialize(root, ChannelDetailWire.GroupChannelDetailWire.class);
        };
    }
}
