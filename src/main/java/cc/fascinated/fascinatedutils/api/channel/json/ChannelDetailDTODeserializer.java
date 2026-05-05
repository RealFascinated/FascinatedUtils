package cc.fascinated.fascinatedutils.api.channel.json;

import com.google.gson.*;

import java.lang.reflect.Type;

public class ChannelDetailDTODeserializer implements JsonDeserializer<ChannelDetailDTO> {

    @Override
    public ChannelDetailDTO deserialize(JsonElement jsonElement, Type targetType, JsonDeserializationContext context) throws JsonParseException {
        if (jsonElement == null || !jsonElement.isJsonObject()) {
            throw new JsonParseException("Channel detail must be a JSON object.");
        }

        JsonObject root = jsonElement.getAsJsonObject();
        JsonElement typeElement = root.get("type");
        if (typeElement == null) {
            throw new JsonParseException("Channel detail is missing a type discriminator.");
        }

        ChannelKindDTO channelType = context.deserialize(typeElement, ChannelKindDTO.class);
        if (channelType == null) {
            throw new JsonParseException("Channel detail has an unknown type discriminator.");
        }

        return switch (channelType) {
            case DM -> context.deserialize(root, ChannelDetailDTO.DmChannelDetailDTO.class);
            case GROUP -> context.deserialize(root, ChannelDetailDTO.GroupChannelDetailDTO.class);
        };
    }
}
