package cc.fascinated.fascinatedutils.api;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import cc.fascinated.fascinatedutils.api.ws.GatewayHandler;
import cc.fascinated.fascinatedutils.api.ws.GatewayOpcode;
import cc.fascinated.fascinatedutils.api.ws.OutboundMessage;
import cc.fascinated.fascinatedutils.api.ws.handlers.*;
import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.AlumiteEnvironment;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class AlumiteGateway implements WebSocket.Listener {

    private static final int CLOSE_AUTH_FAILURE = 4001;
    private static final int MAX_RECONNECT_DELAY_MS = 30_000;

    private final HttpClient httpClient;
    private final Supplier<String> tokenSupplier;
    private final Runnable onAuthExpired;
    private final Gson parseGson;
    private final Map<GatewayOpcode, GatewayHandler> handlers = new EnumMap<>(GatewayOpcode.class);
    private final StringBuilder messageBuffer = new StringBuilder();
    private volatile WebSocket ws;
    private volatile boolean running;
    private int reconnectDelayMs = 1_000;

    AlumiteGateway(HttpClient httpClient, Supplier<String> tokenSupplier, Runnable onAuthExpired, Gson parseGson) {
        this.httpClient = httpClient;
        this.tokenSupplier = tokenSupplier;
        this.onAuthExpired = onAuthExpired;
        this.parseGson = parseGson;
        registerHandlers();
    }

    private void registerHandlers() {
        handlers.put(GatewayOpcode.HELLO, new HelloHandler(tokenSupplier));
        handlers.put(GatewayOpcode.HEARTBEAT, new HeartbeatHandler());
        handlers.put(GatewayOpcode.AUTH_ACK, new AuthAckHandler(() -> reconnectDelayMs = 1_000));
        handlers.put(GatewayOpcode.FRIEND_ADD, new FriendAddHandler());
        handlers.put(GatewayOpcode.FRIEND_REMOVE, new FriendRemoveHandler());
        handlers.put(GatewayOpcode.FRIEND_REQUEST_INCOMING, new FriendRequestIncomingHandler());
        handlers.put(GatewayOpcode.FRIEND_REQUEST_REMOVED, new FriendRequestRemovedHandler());
        handlers.put(GatewayOpcode.USER_UPDATE, new UserUpdateHandler());
        handlers.put(GatewayOpcode.CHANNEL_CREATE, new ChannelCreateHandler());
        handlers.put(GatewayOpcode.CHANNEL_REMOVE, new ChannelRemoveHandler());
        handlers.put(GatewayOpcode.MESSAGE_CREATE, new MessageCreateHandler());
        handlers.put(GatewayOpcode.MESSAGE_UPDATE, new MessageUpdateHandler());
        handlers.put(GatewayOpcode.MESSAGE_DELETE, new MessageDeleteHandler());
    }

    void connect() {
        running = true;
        reconnectDelayMs = 1_000;
        doConnect();
    }

    void disconnect() {
        running = false;
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "").whenComplete((_, _) -> ws = null);
        }
    }

    void send(OutboundMessage message) {
        if (ws != null) {
            ws.sendText(message.toJson(), true);
        }
    }

    private void doConnect() {
        String token = tokenSupplier.get();
        if (token == null || token.isBlank()) {
            Client.LOG.warn("[AlumiteGateway] No access token available, skipping connect.");
            return;
        }
        httpClient.newWebSocketBuilder().buildAsync(URI.create(AlumiteEnvironment.GATEWAY_URL + "/gateway"), this).whenComplete((socket, throwable) -> {
            if (throwable != null) {
                Client.LOG.warn("[AlumiteGateway] Connection failed: {}", throwable.getMessage());
                scheduleReconnect();
            }
            else {
                ws = socket;
            }
        });
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        ws = webSocket;
        Client.LOG.info("[AlumiteGateway] Connection opened.");
        messageBuffer.setLength(0);
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        messageBuffer.append(data);
        if (last) {
            dispatch(messageBuffer.toString());
            messageBuffer.setLength(0);
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        ws = null;
        Client.LOG.info("[AlumiteGateway] Connection closed ({}).", statusCode);
        if (statusCode == CLOSE_AUTH_FAILURE) {
            running = false;
            onAuthExpired.run();
        }
        else if (running) {
            scheduleReconnect();
        }
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        ws = null;
        Client.LOG.warn("[AlumiteGateway] Error: {}", error.getMessage());
        if (running) {
            scheduleReconnect();
        }
    }

    private void dispatch(String raw) {
        try {
            JsonObject obj = parseGson.fromJson(raw, JsonObject.class);
            GatewayOpcode opcode = GatewayOpcode.fromId(obj.get("op").getAsInt());
            if (opcode == null) {
                return;
            }
            GatewayHandler handler = handlers.get(opcode);
            if (handler == null) {
                return;
            }
            JsonElement data = obj.has("data") ? obj.get("data") : JsonNull.INSTANCE;
            handler.handle(this::send, data);
        } catch (Exception exception) {
            Client.LOG.warn("[AlumiteGateway] Failed to dispatch message: {}", exception.getMessage());
        }
    }

    private void scheduleReconnect() {
        int delay = reconnectDelayMs;
        reconnectDelayMs = Math.min(reconnectDelayMs * 2, MAX_RECONNECT_DELAY_MS);
        Client.LOG.info("[AlumiteGateway] Reconnecting in {}ms...", delay);
        FascinatedUtils.SCHEDULED_POOL.schedule(this::doConnect, delay, TimeUnit.MILLISECONDS);
    }
}
