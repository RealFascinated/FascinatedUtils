package cc.fascinated.fascinatedutils.api;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.api.auth.json.*;
import cc.fascinated.fascinatedutils.api.internal.AlumiteHttpClient;
import cc.fascinated.fascinatedutils.api.user.json.PublicUserDTO;
import cc.fascinated.fascinatedutils.api.user.json.UserDTO;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import lombok.Getter;
import lombok.experimental.Accessors;
import meteordevelopment.orbit.EventHandler;

import java.net.http.HttpClient;
import java.time.Duration;

@Accessors(fluent = true)
public class Alumite {

    public static Alumite INSTANCE;

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    @Getter
    private final AlumiteHttpClient http;
    private final AlumiteAuthManager authManager;

    public Alumite() {
        INSTANCE = this;
        this.authManager = new AlumiteAuthManager(new AlumiteTokenStore(), this);
        this.http = new AlumiteHttpClient(httpClient, Constants.GSON, authManager::accessToken, authManager::tryRefreshActive);
    }

    public PublicUserDTO fetchUser(String userId) throws AlumiteApiException {
        return http.get("/users/" + userId).execute(PublicUserDTO.class);
    }

    public RefreshResponseDTO refreshTokens(String refreshToken) {
        return http.post("/auth/refresh").unauthenticated().body(new RefreshRequestDTO(refreshToken)).execute(RefreshResponseDTO.class);
    }

    public ChallengeResponseDTO requestChallenge() {
        return http.post("/auth/challenge").unauthenticated().rawBody("{}").execute(ChallengeResponseDTO.class);
    }

    public VerifyResponseDTO verifyMinecraft(VerifyRequestDTO request) {
        return http.post("/auth/minecraft/verify").unauthenticated().body(request).execute(VerifyResponseDTO.class);
    }

    public UserDTO getSelfUser() {
        return http.get("/users/@me").execute(UserDTO.class);
    }

    @EventHandler
    private void alumite$onClientStarted(ClientStartedEvent event) {
        authManager.authenticate(event.minecraftClient());
    }
}
