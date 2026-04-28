package cc.fascinated.fascinatedutils.api.ws.impl;

import cc.fascinated.fascinatedutils.api.ws.GatewayOpcode;
import cc.fascinated.fascinatedutils.api.ws.OutboundMessage;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class C2SAuthMessage extends OutboundMessage {

    private final String token;

    public C2SAuthMessage(String token) {
        super(GatewayOpcode.AUTH);
        this.token = token;
    }

    @Override
    protected JsonElement data() {
        JsonObject data = new JsonObject();
        data.addProperty("token", token);
        return data;
    }
}
