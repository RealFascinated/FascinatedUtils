package cc.fascinated.fascinatedutils.api;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.api.auth.json.ChallengeResponseDTO;
import cc.fascinated.fascinatedutils.api.auth.json.RefreshResponseDTO;
import cc.fascinated.fascinatedutils.api.auth.json.VerifyRequestDTO;
import cc.fascinated.fascinatedutils.api.auth.json.VerifyResponseDTO;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.AlumiteAuthenticatedEvent;
import net.minecraft.client.Minecraft;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class AlumiteAuthManager {

    record SessionState(String accountKey, String accessToken, String refreshToken) {}

    private final AtomicReference<SessionState> session = new AtomicReference<>();
    private final AlumiteTokenStore tokenStore;
    private volatile ScheduledFuture<?> tokenRefreshTask;

    private Alumite alumite;
    private AlumiteGateway gateway;

    AlumiteAuthManager(AlumiteTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    void init(Alumite alumite, AlumiteGateway gateway) {
        this.alumite = alumite;
        this.gateway = gateway;
    }

    String accessToken() {
        SessionState state = session.get();
        return state != null ? state.accessToken() : null;
    }

    String refreshToken() {
        SessionState state = session.get();
        return state != null ? state.refreshToken() : null;
    }

    void authenticate(Minecraft minecraftClient) {
        String accountKey = minecraftClient.getUser().getProfileId().toString();
        session.set(new SessionState(accountKey, null, null));

        AlumiteTokenStore.StoredSession stored = tokenStore.load(accountKey);
        if (stored != null) {
            if (isAccessTokenValid(stored.accessExpiresAt())) {
                session.set(new SessionState(accountKey, stored.accessToken(), stored.refreshToken()));
                alumite.users().updateSelfUser();
                scheduleTokenRefresh(stored.accessExpiresAt());
                Client.LOG.info("[Alumite] Resumed session from stored access token.");
                gateway.connect();
                FascinatedEventBus.INSTANCE.post(new AlumiteAuthenticatedEvent());
                return;
            }
            if (tryRefresh(accountKey, stored.refreshToken())) {
                gateway.connect();
                FascinatedEventBus.INSTANCE.post(new AlumiteAuthenticatedEvent());
                return;
            }
        }
        tokenStore.clear(accountKey);
        performFullLogin(minecraftClient);
        if (accessToken() != null) {
            gateway.connect();
            FascinatedEventBus.INSTANCE.post(new AlumiteAuthenticatedEvent());
        }
    }

    boolean tryRefreshActive() {
        SessionState state = session.get();
        return state != null && state.refreshToken() != null && tryRefresh(state.accountKey(), state.refreshToken());
    }

    void onGatewayAuthExpired() {
        Client.LOG.info("[Alumite] Gateway refresh token expired, re-authenticating...");
        SessionState staleSession = session.getAndUpdate(state -> state != null ? new SessionState(state.accountKey(), null, null) : null);
        Constants.EXECUTORS.execute(() -> {
            if (staleSession != null && staleSession.refreshToken() != null && tryRefresh(staleSession.accountKey(), staleSession.refreshToken())) {
                gateway.connect();
                return;
            }
            if (staleSession != null) {
                tokenStore.clear(staleSession.accountKey());
            }
            performFullLogin(Minecraft.getInstance());
            if (refreshToken() != null) {
                gateway.connect();
            }
        });
    }

    private boolean tryRefresh(String accountKey, String storedRefreshToken) {
        try {
            RefreshResponseDTO refreshResponse = alumite.refreshTokens(storedRefreshToken);
            session.set(new SessionState(accountKey, refreshResponse.accessToken(), refreshResponse.refreshToken()));
            alumite.users().updateSelfUser();
            tokenStore.save(accountKey, refreshResponse.refreshToken(), refreshResponse.accessToken(), refreshResponse.accessExpiresAt());
            scheduleTokenRefresh(refreshResponse.accessExpiresAt());
            Client.LOG.info("[Alumite] Session refreshed.");
            return true;
        } catch (Exception exception) {
            session.updateAndGet(state -> state != null ? new SessionState(state.accountKey(), null, null) : null);
            Client.LOG.warn("[Alumite] Refresh failed: {}", exception.getMessage());
            return false;
        }
    }

    private void performFullLogin(Minecraft minecraftClient) {
        String minecraftAccessToken = minecraftClient.getUser().getAccessToken();
        if (minecraftAccessToken.length() < 16) {
            Client.LOG.info("[Alumite] Skipping auth — offline/dev session detected.");
            return;
        }
        try {
            ChallengeResponseDTO challenge = alumite.requestChallenge();
            VerifyResponseDTO result = alumite.verifyMinecraft(new VerifyRequestDTO(challenge.challengeId(), challenge.nonce(), minecraftAccessToken));
            String accountKey = session.get().accountKey();
            session.set(new SessionState(accountKey, result.accessToken(), result.refreshToken()));
            alumite.users().updateSelfUser();
            tokenStore.save(accountKey, result.refreshToken(), result.accessToken(), result.accessExpiresAt());
            scheduleTokenRefresh(result.accessExpiresAt());
            Client.LOG.info("[Alumite] Authenticated as {}", alumite.users().selfUser().user().minecraftName());
        } catch (Exception exception) {
            Client.LOG.warn("[Alumite] Login failed: {}", exception.getMessage());
        }
    }

    private void scheduleTokenRefresh(String accessExpiresAt) {
        if (tokenRefreshTask != null) {
            tokenRefreshTask.cancel(false);
        }
        if (accessExpiresAt == null) {
            return;
        }
        try {
            long delayMs = Math.max(0L, Instant.parse(accessExpiresAt).toEpochMilli() - System.currentTimeMillis() - 60_000L);
            SessionState capturedSession = session.get();
            tokenRefreshTask = Constants.SCHEDULED_POOL.schedule(() -> {
                if (capturedSession != null && capturedSession.refreshToken() != null) {
                    Client.LOG.info("[Alumite] Refreshing access token...");
                    tryRefresh(capturedSession.accountKey(), capturedSession.refreshToken());
                }
            }, delayMs, TimeUnit.MILLISECONDS);
        } catch (Exception exception) {
            Client.LOG.warn("[Alumite] Failed to schedule token refresh: {}", exception.getMessage());
        }
    }



    private boolean isAccessTokenValid(String accessExpiresAt) {
        if (accessExpiresAt == null) {
            return false;
        }
        try {
            return Instant.parse(accessExpiresAt).toEpochMilli() - System.currentTimeMillis() > 60_000L;
        } catch (Exception exception) {
            return false;
        }
    }
}
