package cc.fascinated.fascinatedutils.api.internal;

import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.Errors;
import cc.fascinated.fascinatedutils.common.AlumiteEnvironment;
import cc.fascinated.fascinatedutils.common.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Accessors(fluent = true)
public final class AlumiteHttpClient {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MS = 500L;
    private static final long MAX_RETRY_DELAY_MS = 10_000L;
    private static final int MAX_ERROR_BODY_CHARS = 200;

    private final HttpClient httpClient;
    @Getter
    private final Gson gson;
    private final Supplier<String> accessTokenSupplier;
    private final BooleanSupplier refreshActiveToken;

    public AlumiteHttpClient(HttpClient httpClient, Gson gson, Supplier<String> accessTokenSupplier, BooleanSupplier refreshActiveToken) {
        this.httpClient = httpClient;
        this.gson = gson;
        this.accessTokenSupplier = accessTokenSupplier;
        this.refreshActiveToken = refreshActiveToken;
    }

    public RequestBuilder get(String path) { return new RequestBuilder("GET", path); }
    public RequestBuilder post(String path) { return new RequestBuilder("POST", path); }

    public final class RequestBuilder {
        private final String method;
        private final String path;
        private String body;
        private boolean authenticated = true;

        private RequestBuilder(String method, String path) {
            this.method = method;
            this.path = path;
        }

        public RequestBuilder body(Object obj) {
            this.body = gson.toJson(obj);
            return this;
        }

        public RequestBuilder rawBody(String json) {
            this.body = json;
            return this;
        }

        public RequestBuilder unauthenticated() {
            this.authenticated = false;
            return this;
        }

        public <T> T execute(Class<T> type) {
            HttpResponse<String> response = send();
            if (response.statusCode() != 200) {
                throw parseApiException(response);
            }
            return gson.fromJson(response.body(), type);
        }

        private HttpResponse<String> send() {
            if (authenticated) {
                return executeWithRetry(() -> buildRequest(path, method, body, requireAccessToken()));
            }
            return sendWithBackoff(() -> buildRequest(path, method, body, null));
        }
    }

    private HttpResponse<String> executeWithRetry(Supplier<HttpRequest> buildRequest) {
        HttpResponse<String> response = sendWithBackoff(buildRequest);
        if (response.statusCode() == 401 && refreshActiveToken.getAsBoolean()) {
            response = sendWithBackoff(buildRequest);
        }
        return response;
    }

    /**
     * Sends the request, retrying a bounded number of times on 429 and 5xx responses.
     *
     * @param buildRequest builds a fresh request for each attempt
     * @return the last response received
     */
    private HttpResponse<String> sendWithBackoff(Supplier<HttpRequest> buildRequest) {
        HttpResponse<String> response = sendOnce(buildRequest.get());
        for (int attempt = 1; attempt < MAX_ATTEMPTS && isRetryableStatus(response.statusCode()); attempt++) {
            sleepBeforeRetry(retryDelayMs(response, attempt));
            response = sendOnce(buildRequest.get());
        }
        return response;
    }

    private HttpResponse<String> sendOnce(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new AlumiteApiException(null, "Request failed: " + exception.getMessage(), 0, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AlumiteApiException(null, "Request interrupted", 0, exception);
        }
    }

    private boolean isRetryableStatus(int status) {
        return status == 429 || status >= 500;
    }

    private long retryDelayMs(HttpResponse<String> response, int attempt) {
        long backoffMs = BASE_BACKOFF_MS << (attempt - 1);
        long retryAfterMs = response.headers().firstValue("Retry-After").map(value -> {
            try {
                return Long.parseLong(value.strip()) * 1000L;
            } catch (NumberFormatException exception) {
                return 0L;
            }
        }).orElse(0L);
        return Math.min(MAX_RETRY_DELAY_MS, Math.max(backoffMs, retryAfterMs));
    }

    private void sleepBeforeRetry(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AlumiteApiException(null, "Request interrupted", 0, exception);
        }
    }

    private HttpRequest buildRequest(String path, String method, String body, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(AlumiteEnvironment.API_BASE_URL + path))
                .header("Content-Type", "application/json")
                .header("User-Agent", AlumiteEnvironment.USER_AGENT)
                .timeout(Duration.ofSeconds(15))
                .method(method, body != null ? HttpRequest.BodyPublishers.ofString(body) : HttpRequest.BodyPublishers.noBody());
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder.build();
    }

    private String requireAccessToken() {
        String token = accessTokenSupplier.get();
        if (token == null || token.isBlank()) {
            throw new AlumiteApiException(Errors.INVALID_ACCESS_TOKEN, Errors.INVALID_ACCESS_TOKEN.getDisplayText(), 401, null);
        }
        return token;
    }

    private AlumiteApiException parseApiException(HttpResponse<String> response) {
        int status = response.statusCode();
        String responseBody = response.body();
        try {
            JsonObject root = gson.fromJson(responseBody, JsonObject.class);
            Errors error = Errors.fromCode(JsonUtils.stringMember(root, "error"));
            if (error == null) {
                error = Errors.fromCode(JsonUtils.stringMember(root, "code"));
            }
            String message = JsonUtils.stringMember(root, "message");
            if ((message == null || message.isBlank()) && error != null) {
                message = error.getDisplayText();
            }
            if (message == null || message.isBlank()) {
                message = "HTTP " + status + ": " + truncate(responseBody);
            }
            return new AlumiteApiException(error, message, status, null);
        } catch (Exception parseFailure) {
            return new AlumiteApiException(null, "HTTP " + status + ": " + truncate(responseBody), status, parseFailure);
        }
    }

    private String truncate(String text) {
        if (text == null) {
            return "<empty body>";
        }
        String stripped = text.strip();
        return stripped.length() <= MAX_ERROR_BODY_CHARS ? stripped : stripped.substring(0, MAX_ERROR_BODY_CHARS) + "...";
    }
}
