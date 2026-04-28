package cc.fascinated.fascinatedutils.api.ws.impl;

import cc.fascinated.fascinatedutils.api.ws.GatewayOpcode;
import cc.fascinated.fascinatedutils.api.ws.OutboundMessage;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class C2SHeartbeatAckMessage extends OutboundMessage {

    public C2SHeartbeatAckMessage() {
        super(GatewayOpcode.HEARTBEAT_ACK);
    }

    @Override
    protected JsonElement data() {
        return new JsonObject();
    }
}
