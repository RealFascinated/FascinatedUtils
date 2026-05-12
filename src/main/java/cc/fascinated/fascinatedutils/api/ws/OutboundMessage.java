package cc.fascinated.fascinatedutils.api.ws;

import cc.fascinated.fascinatedutils.Constants;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public abstract class OutboundMessage {

    private final GatewayOpcode op;

    protected OutboundMessage(GatewayOpcode op) {
        this.op = op;
    }

    /**
     * Returns the data payload for this message, or {@link JsonNull#INSTANCE} if there is none.
     */
    public JsonElement data() {
        return JsonNull.INSTANCE;
    }

    /**
     * Serializes this message to the wire format: {@code {"op":<int>,"data":<value>}}.
     *
     * @return JSON string ready to send over the WebSocket
     */
    public final String toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("op", op.id);
        JsonElement data = data();
        if (!data.isJsonNull()) {
            obj.add("data", data);
        }
        return Constants.GSON.toJson(obj);
    }
}
