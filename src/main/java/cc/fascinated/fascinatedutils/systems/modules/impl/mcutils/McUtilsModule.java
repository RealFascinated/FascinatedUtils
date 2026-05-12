package cc.fascinated.fascinatedutils.systems.modules.impl.mcutils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.packet.PacketReceiveEvent;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.HudWidgetAppearanceBuilders;
import cc.fascinated.fascinatedutils.systems.modules.impl.mcutils.hud.McUtilsHudPanel;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;

public class McUtilsModule extends HudHostModule {

    private static final int MAX_UUIDS = 50_000;
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final ConcurrentLinkedQueue<UUID> toSubmit = new ConcurrentLinkedQueue<>();
    private final LinkedHashSet<UUID> submittedUuids = new LinkedHashSet<>(MAX_UUIDS * 2);

    private volatile long initialCount = -1;
    private volatile long currentCount = -1;

    private final BooleanSetting showBackground = HudWidgetAppearanceBuilders.showBackground().build();
    private final BooleanSetting roundedCorners = HudWidgetAppearanceBuilders.roundedCorners().build();
    private final SliderSetting roundingRadius = HudWidgetAppearanceBuilders.roundingRadius().build();
    private final BooleanSetting showBorder = HudWidgetAppearanceBuilders.showBorder().build();
    private final SliderSetting borderThickness = HudWidgetAppearanceBuilders.borderThickness().build();
    private final ColorSetting backgroundColor = HudWidgetAppearanceBuilders.backgroundColor().build();
    private final ColorSetting borderColor = HudWidgetAppearanceBuilders.borderColor().build();
    private final BooleanSetting removeMinimumWidth = HudWidgetAppearanceBuilders.removeMinimumWidth().build();
    private final SliderSetting padding = HudWidgetAppearanceBuilders.padding().build();
    private final BooleanSetting textShadow = HudWidgetAppearanceBuilders.textShadow().build();

    public McUtilsModule() {
        super("mcutils", "McUtils", HudDefaults.builder().defaultState(true).alwaysShowPanelToggles(true).build());
        addSetting(showBackground);
        addSetting(roundedCorners);
        addSetting(showBorder);
        addSetting(roundingRadius);
        addSetting(borderThickness);
        addSetting(backgroundColor);
        addSetting(borderColor);
        showBackground.addSubSetting(backgroundColor);
        roundedCorners.addSubSetting(roundingRadius);
        showBorder.addSubSetting(borderThickness);
        showBorder.addSubSetting(borderColor);
        addSetting(removeMinimumWidth);
        addSetting(padding);
        addSetting(textShadow);
        registerHudPanel(new McUtilsHudPanel(this));

        Constants.SCHEDULED_POOL.scheduleAtFixedRate(this::submitPending, 0, 1, TimeUnit.MINUTES);
        Constants.SCHEDULED_POOL.scheduleAtFixedRate(this::fetchStats, 0, 10, TimeUnit.SECONDS);
        FascinatedEventBus.INSTANCE.subscribe(this);
    }

    private void submitPending() {
        try {
            // Drain the queue into a local list, skipping already-submitted UUIDs.
            List<UUID> sending = new ArrayList<>();
            UUID uuid;
            while ((uuid = toSubmit.poll()) != null) {
                if (!submittedUuids.contains(uuid)) {
                    sending.add(uuid);
                }
            }

            if (sending.isEmpty()) {
                return;
            }

            // Evict oldest entries before adding new ones so we never exceed the cap.
            int overflow = (submittedUuids.size() + sending.size()) - MAX_UUIDS;
            if (overflow > 0) {
                var iterator = submittedUuids.iterator();
                for (int i = 0; i < overflow && iterator.hasNext(); i++) {
                    iterator.next();
                    iterator.remove();
                }
            }
            submittedUuids.addAll(sending);

            Minecraft client = Minecraft.getInstance();
            GameProfile player = client.getGameProfile();
            if (player == null) {
                return;
            }

            SubmitPlayers payload = new SubmitPlayers(sending, player.id());
            String json = Constants.GSON.toJson(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://mc.fascinated.cc/api/players/submit"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            Client.LOG.info("Submitted {} UUIDs!", sending.size());
        } catch (Exception e) {
            Client.LOG.error("Failed to submit UUIDs to McUtils", e);
        }
    }

    private void fetchStats() {
        try {
            Minecraft client = Minecraft.getInstance();
            GameProfile player = client.getGameProfile();
            if (player == null) {
                return;
            }
            String uuid = player.id().toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://mc.fascinated.cc/api/players/" + uuid))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = Constants.GSON.fromJson(response.body(), JsonObject.class);
            long count = json.get("submittedUuids").getAsLong();

            if (initialCount == -1) {
                initialCount = count;
            }
            currentCount = count;
        } catch (Exception exception) {
            Client.LOG.error("Failed to fetch McUtils player stats", exception);
        }
    }

    public long getSubmittedUuids() {
        return currentCount;
    }

    public long getSessionDelta() {
        if (initialCount == -1 || currentCount == -1) {
            return 0;
        }
        return currentCount - initialCount;
    }

    @EventHandler
    public void onPacket(PacketReceiveEvent event) {
        Packet<?> packet = event.packet();
        if (!(packet instanceof ClientboundPlayerInfoUpdatePacket infoUpdatePacket)) {
            return;
        }

        for (ClientboundPlayerInfoUpdatePacket.Entry entry : infoUpdatePacket.entries()) {
            GameProfile profile = entry.profile();
            if (profile == null) {
                continue;
            }
            UUID id = profile.id();
            // Skip Geyser (Bedrock) players — their UUIDs start with 00000000.
            if (id.toString().startsWith("00000000")) {
                continue;
            }
            toSubmit.add(id);
        }
    }

    private record SubmitPlayers(List<UUID> uuids, UUID submittedBy) { }
}