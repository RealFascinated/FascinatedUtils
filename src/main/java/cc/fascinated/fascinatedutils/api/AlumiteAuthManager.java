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
import java.util.function.Supplier;

class AlumiteAuthManager {

    private static final int MAX_NETWORK_ATTEMPTS = 3;
    private static final long NETWORK_BACKOFF_MS = 1_000L;

    record SessionState(String accountKey, String accessToken, String refreshToken) {}

    private final AtomicReference<SessionState> session = new AtomicReference<>();
    private final Object refreshLock = new Object();
    private final AlumiteTokenStore tokenStore;
    private volatile ScheduledFuture<?> tokenRefreshTask;

    private final Alumite alumite;

    AlumiteAuthManager(AlumiteTokenStore tokenStore, Alumite alumite) {
        this.tokenStore = tokenStore;
        this.alumite = alumite;
    }

    String accessToken() {
        SessionState state = session.get();
        return state != null ? state.accessToken() : null;
    }

    /**
     * Starts authentication on the shared executor so the client thread is never blocked on network calls.
     *
     * @param minecraftClient the running Minecraft client
     */
    void authenticate(Minecraft minecraftClient) {
        String accountKey = minecraftClient.getUser().getProfileId().toString();
        String minecraftAccessToken = minecraftClient.getUser().getAccessToken();
        Constants.EXECUTORS.execute(() -> {
            try {
                authenticateBlocking(accountKey, minecraftAccessToken);
            } catch (Exception exception) {
                Client.LOG.warn("[Alumite] Authentication failed: {}", exception.getMessage());
            }
        });
    }

    private void authenticateBlocking(String accountKey, String minecraftAccessToken) {
        session.set(new SessionState(accountKey, null, null));

        AlumiteTokenStore.StoredSession stored = tokenStore.load(accountKey);
        if (stored != null) {
            if (isAccessTokenValid(stored.accessExpiresAt())) {
                session.set(new SessionState(accountKey, stored.accessToken(), stored.refreshToken()));
                scheduleTokenRefresh(stored.accessExpiresAt());
                Client.LOG.info("[Alumite] Resumed session from stored access token.");
                FascinatedEventBus.INSTANCE.post(new AlumiteAuthenticatedEvent());
                return;
            }
            if (tryRefresh(accountKey, stored.refreshToken())) {
                FascinatedEventBus.INSTANCE.post(new AlumiteAuthenticatedEvent());
                return;
            }
        }
        tokenStore.clear(accountKey);
        performFullLogin(accountKey, minecraftAccessToken);
        if (accessToken() != null) {
            FascinatedEventBus.INSTANCE.post(new AlumiteAuthenticatedEvent());
        }
    }

    boolean tryRefreshActive() {
        SessionState state = session.get();
        return state != null && state.refreshToken() != null && tryRefresh(state.accountKey(), state.refreshToken());
    }

    /**
     * Refreshes the session using the given refresh token. Refreshes are serialized; if the active session has
     * already rotated past the given token the refresh is treated as done, and the session is only cleared when the
     * failing token is still the active one.
     *
     * @param accountKey the account the session belongs to
     * @param refreshToken the refresh token to redeem
     * @return true if the session holds a usable access token afterwards
     */
    private boolean tryRefresh(String accountKey, String refreshToken) {
        synchronized (refreshLock) {
            SessionState active = session.get();
            if (active != null && active.refreshToken() != null && !active.refreshToken().equals(refreshToken)) {
                return active.accessToken() != null;
            }
            try {
                RefreshResponseDTO refreshResponse = withNetworkRetry(() -> alumite.refreshTokens(refreshToken));
                session.set(new SessionState(accountKey, refreshResponse.accessToken(), refreshResponse.refreshToken()));
                tokenStore.save(accountKey, refreshResponse.refreshToken(), refreshResponse.accessToken(), refreshResponse.accessExpiresAt());
                scheduleTokenRefresh(refreshResponse.accessExpiresAt());
                Client.LOG.info("[Alumite] Session refreshed.");
                return true;
            } catch (Exception exception) {
                session.updateAndGet(state -> {
                    if (state == null) {
                        return null;
                    }
                    boolean failingTokenIsActive = state.refreshToken() == null || state.refreshToken().equals(refreshToken);
                    return failingTokenIsActive ? new SessionState(state.accountKey(), null, null) : state;
                });
                Client.LOG.warn("[Alumite] Refresh failed: {}", exception.getMessage());
                return false;
            }
        }
    }

    private void performFullLogin(String accountKey, String minecraftAccessToken) {
        if (minecraftAccessToken.length() < 16) {
            Client.LOG.info("[Alumite] Skipping auth — offline/dev session detected.");
            return;
        }
        try {
            ChallengeResponseDTO challenge = withNetworkRetry(alumite::requestChallenge);
            VerifyResponseDTO result = withNetworkRetry(() -> alumite.verifyMinecraft(new VerifyRequestDTO(challenge.challengeId(), challenge.nonce(), minecraftAccessToken)));
            session.set(new SessionState(accountKey, result.accessToken(), result.refreshToken()));
            tokenStore.save(accountKey, result.refreshToken(), result.accessToken(), result.accessExpiresAt());
            scheduleTokenRefresh(result.accessExpiresAt());
            Client.LOG.info("[Alumite] Authenticated as {}", alumite.getSelfUser().minecraftName());
        } catch (Exception exception) {
            Client.LOG.warn("[Alumite] Login failed: {}", exception.getMessage());
        }
    }

    /**
     * Retries a call a bounded number of times with backoff when it fails with a network error. HTTP 429 and 5xx are
     * already retried inside the HTTP client.
     *
     * @param call the request to run
     * @param <T> the result type
     * @return the result of the first successful attempt
     */
    private <T> T withNetworkRetry(Supplier<T> call) {
        for (int attempt = 1; ; attempt++) {
            try {
                return call.get();
            } catch (AlumiteApiException exception) {
                if (exception.getStatusCode() != 0 || attempt >= MAX_NETWORK_ATTEMPTS || Thread.currentThread().isInterrupted()) {
                    throw exception;
                }
                try {
                    Thread.sleep(NETWORK_BACKOFF_MS << (attempt - 1));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw exception;
                }
            }
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
            tokenRefreshTask = Constants.SCHEDULED_POOL.schedule(() -> {
                SessionState current = session.get();
                if (current != null && current.refreshToken() != null) {
                    Client.LOG.info("[Alumite] Refreshing access token...");
                    tryRefresh(current.accountKey(), current.refreshToken());
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
