package cc.fascinated.fascinatedutils.api.ws;

import com.google.gson.JsonElement;

import java.util.function.Consumer;

@FunctionalInterface
public interface GatewayHandler {

    void handle(Consumer<OutboundMessage> send, JsonElement data);
}
