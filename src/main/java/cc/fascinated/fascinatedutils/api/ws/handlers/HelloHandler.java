package cc.fascinated.fascinatedutils.api.ws.handlers;

import cc.fascinated.fascinatedutils.api.ws.GatewayHandler;
import cc.fascinated.fascinatedutils.api.ws.OutboundMessage;
import cc.fascinated.fascinatedutils.api.ws.impl.C2SAuthMessage;
import cc.fascinated.fascinatedutils.client.Client;
import com.google.gson.JsonElement;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class HelloHandler implements GatewayHandler {

    private final Supplier<String> tokenSupplier;

    @Override
    public void handle(Consumer<OutboundMessage> send, JsonElement data) {
        int heartbeatInterval = data.getAsJsonObject().get("heartbeatInterval").getAsInt();
        Client.LOG.info("[AlumiteGateway] HELLO received (heartbeatInterval={}ms), authenticating...", heartbeatInterval);
        send.accept(new C2SAuthMessage(tokenSupplier.get()));
    }
}
