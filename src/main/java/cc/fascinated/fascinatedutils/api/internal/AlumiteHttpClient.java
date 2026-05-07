package cc.fascinated.fascinatedutils.api.internal;

import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.Errors;
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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Accessors(fluent = true)
public final class AlumiteHttpClient {

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
    public RequestBuilder patch(String path) { return new RequestBuilder("PATCH", path); }
    public RequestBuilder delete(String path) { return new RequestBuilder("DELETE", path); }

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
                throw parseApiException(response.body());
            }
            return gson.fromJson(response.body(), type);
        }

        public <T> List<T> executeList(Class<T> type) {
            HttpResponse<String> response = send();
            if (response.statusCode() != 200) {
                throw parseApiException(response.body());
            }
            return gson.fromJson(response.body(), TypeToken.getParameterized(List.class, type).getType());
        }

        public void executeVoid() {
            HttpResponse<String> response = send();
            int status = response.statusCode();
            if (status != 200 && status != 204) {
                throw parseApiException(response.body());
            }
        }

        @SneakyThrows
        private HttpResponse<String> send() {
            if (authenticated) {
                return executeWithRetry(() -> buildRequest(path, method, body, requireAccessToken()));
            }
            return httpClient.send(buildRequest(path, method, body, null), HttpResponse.BodyHandlers.ofString());
        }
    }

    @SneakyThrows
    public <T> T postMultipart(String path, byte[] fileData, String filename, Class<T> type) {
        String boundary = UUID.randomUUID().toString().replace("-", "");
        byte[] body = buildMultipartBody(boundary, fileData, filename);
        HttpResponse<String> response = executeWithRetry(() -> HttpRequest.newBuilder()
                .uri(URI.create(AlumiteEnvironment.API_BASE_URL + path))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Authorization", "Bearer " + requireAccessToken())
                .header("User-Agent", AlumiteEnvironment.USER_AGENT)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build());
        if (response.statusCode() != 200) {
            throw parseApiException(response.body());
        }
        return gson.fromJson(response.body(), type);
    }

    @SneakyThrows
    private HttpResponse<String> executeWithRetry(Supplier<HttpRequest> buildRequest) {
        HttpResponse<String> response = httpClient.send(buildRequest.get(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401 && refreshActiveToken.getAsBoolean()) {
            response = httpClient.send(buildRequest.get(), HttpResponse.BodyHandlers.ofString());
        }
        return response;
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

    private AlumiteApiException parseApiException(String responseBody) {
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
            return new AlumiteApiException(error, message);
        } catch (Exception ignored) {
            return new AlumiteApiException(null, null);
        }
    }

    private static byte[] buildMultipartBody(String boundary, byte[] fileData, String filename) {
        String crlf = "\r\n";
        byte[] filePart = ("--" + boundary + crlf
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"" + crlf
                + "Content-Type: application/octet-stream" + crlf
                + crlf).getBytes(StandardCharsets.UTF_8);
        byte[] namePart = (crlf + "--" + boundary + crlf
                + "Content-Disposition: form-data; name=\"name\"" + crlf
                + crlf).getBytes(StandardCharsets.UTF_8);
        byte[] nameValue = filename.getBytes(StandardCharsets.UTF_8);
        byte[] end = (crlf + "--" + boundary + "--" + crlf).getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[filePart.length + fileData.length + namePart.length + nameValue.length + end.length];
        int offset = 0;
        System.arraycopy(filePart, 0, result, offset, filePart.length); offset += filePart.length;
        System.arraycopy(fileData, 0, result, offset, fileData.length); offset += fileData.length;
        System.arraycopy(namePart, 0, result, offset, namePart.length); offset += namePart.length;
        System.arraycopy(nameValue, 0, result, offset, nameValue.length); offset += nameValue.length;
        System.arraycopy(end, 0, result, offset, end.length);
        return result;
    }
}
