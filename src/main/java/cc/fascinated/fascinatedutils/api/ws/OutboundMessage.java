package cc.fascinated.fascinatedutils.api.ws;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public abstract class OutboundMessage {

    private static final Gson GSON = new Gson();

    private final GatewayOpcode op;

    protected OutboundMessage(GatewayOpcode op) {
        this.op = op;
    }

    /**
     * Returns the data payload for this message, or {@link JsonNull#INSTANCE} if there is none.
     */
    protected JsonElement data() {
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
        return GSON.toJson(obj);
    }
}
