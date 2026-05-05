package cc.fascinated.fascinatedutils.api.internal;

import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.Errors;
import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.AlumiteEnvironment;
import cc.fascinated.fascinatedutils.common.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Accessors(fluent = true)
public final class AlumiteHttpClient {

    private final HttpClient httpClient;
    @Getter
    private final Gson gson;
    private final Supplier<String> accessTokenSupplier;
    private final BooleanSupplier refreshActiveToken;

    public AlumiteHttpClient(
            HttpClient httpClient,
            Gson gson,
            Supplier<String> accessTokenSupplier,
            BooleanSupplier refreshActiveToken
    ) {
        this.httpClient = httpClient;
        this.gson = gson;
        this.accessTokenSupplier = accessTokenSupplier;
        this.refreshActiveToken = refreshActiveToken;
    }

    public HttpResponse<String> sendAuthorizedChecked(String method, String path, String body, String actionName, String fallbackMessage) {
        try {
            HttpResponse<String> response = sendAuthorized(method, path, body);
            if (response.statusCode() != 200) {
                Client.LOG.warn("[Alumite] {} failed with status {}: {}", actionName, response.statusCode(), response.body());
                throw parseApiException(response.body(), fallbackMessage);
            }
            return response;
        } catch (AlumiteApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw wrapRequestException(actionName, exception, fallbackMessage);
        }
    }

    public HttpResponse<String> sendAuthorizedExpectNoContent(String method, String path, String body, String actionName, String fallbackMessage) {
        try {
            HttpResponse<String> response = sendAuthorized(method, path, body);
            int status = response.statusCode();
            if (status != 200 && status != 204) {
                Client.LOG.warn("[Alumite] {} failed with status {}: {}", actionName, status, response.body());
                throw parseApiException(response.body(), fallbackMessage);
            }
            return response;
        } catch (AlumiteApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw wrapRequestException(actionName, exception, fallbackMessage);
        }
    }

    @SneakyThrows
    public HttpResponse<String> sendAuthorized(String method, String path, String body) {
        Supplier<HttpRequest> buildRequest = () -> buildRequest(path, method, body, requireAccessToken());
        HttpResponse<String> response = httpClient.send(buildRequest.get(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401 && refreshActiveToken.getAsBoolean()) {
            response = httpClient.send(buildRequest.get(), HttpResponse.BodyHandlers.ofString());
        }
        return response;
    }

    @SneakyThrows
    public HttpResponse<String> sendRaw(String method, String path, String body, String token) {
        return httpClient.send(buildRequest(path, method, body, token), HttpResponse.BodyHandlers.ofString());
    }

    public <T> List<T> getList(String path, Class<T> elementType, String actionName, String fallbackMessage) {
        try {
            HttpResponse<String> response = sendAuthorizedChecked("GET", path, null, actionName, fallbackMessage);
            return gson.fromJson(response.body(), TypeToken.getParameterized(List.class, elementType).getType());
        } catch (AlumiteApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw wrapRequestException(actionName, exception, fallbackMessage);
        }
    }

    public <T> T getObject(String path, Class<T> type, String actionName, String fallbackMessage) {
        try {
            HttpResponse<String> response = sendAuthorizedChecked("GET", path, null, actionName, fallbackMessage);
            return gson.fromJson(response.body(), type);
        } catch (AlumiteApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw wrapRequestException(actionName, exception, fallbackMessage);
        }
    }

    public <T> T postObject(String path, Object request, Class<T> type, String actionName, String fallbackMessage) {
        try {
            HttpResponse<String> response = sendAuthorizedChecked("POST", path, gson.toJson(request), actionName, fallbackMessage);
            return gson.fromJson(response.body(), type);
        } catch (AlumiteApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw wrapRequestException(actionName, exception, fallbackMessage);
        }
    }

    public <T> T patchObject(String path, Object request, Class<T> type, String actionName, String fallbackMessage) {
        try {
            HttpResponse<String> response = sendAuthorizedChecked("PATCH", path, gson.toJson(request), actionName, fallbackMessage);
            return gson.fromJson(response.body(), type);
        } catch (AlumiteApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw wrapRequestException(actionName, exception, fallbackMessage);
        }
    }

    public <T> T deleteObject(String path, Class<T> type, String actionName, String fallbackMessage) {
        try {
            HttpResponse<String> response = sendAuthorizedChecked("DELETE", path, null, actionName, fallbackMessage);
            return gson.fromJson(response.body(), type);
        } catch (AlumiteApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw wrapRequestException(actionName, exception, fallbackMessage);
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
            throw new IllegalStateException("No active Alumite access token available");
        }
        return token;
    }

    private AlumiteApiException parseApiException(String responseBody, String fallbackMessage) {
        try {
            JsonObject root = gson.fromJson(responseBody, JsonObject.class);
            Errors error = Errors.fromCode(JsonUtils.stringMember(root, "error"));
            if (error == null) {
                error = Errors.fromCode(JsonUtils.stringMember(root, "code"));
            }
            String message = JsonUtils.stringMember(root, "message", fallbackMessage);
            if ((message == null || message.isBlank()) && error != null) {
                message = error.getDisplayText();
            }
            return new AlumiteApiException(error, message);
        } catch (Exception ignored) {
            return new AlumiteApiException(null, fallbackMessage);
        }
    }

    private AlumiteApiException wrapRequestException(String actionName, Exception exception, String fallbackMessage) {
        Client.LOG.warn("[Alumite] {} failed: {}", actionName, exception.getMessage());
        return new AlumiteApiException(null, fallbackMessage);
    }
}
