package cc.fascinated.fascinatedutils.api;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

class AlumiteTokenStore {

    record StoredSession(String refreshToken, String accessToken, String accessExpiresAt) {}


    private Path storeDirectory() {
        return ModConfig.getDirectory().resolve("alumite_sessions");
    }

    private Path storePath(String accountKey) {
        return storeDirectory().resolve(sanitizeAccountKey(accountKey));
    }

    StoredSession load(String accountKey) {
        Path path = storePath(accountKey);
        if (!Files.exists(path)) {
            return null;
        }
        try {
            String[] lines = Files.readString(path).strip().split("\n", 3);
            if (lines.length < 3) {
                return null;
            }
            String refreshToken = lines[0].strip();
            String accessToken = lines[1].strip();
            String accessExpiresAt = lines[2].strip();
            if (refreshToken.isEmpty() || accessToken.isEmpty() || accessExpiresAt.isEmpty()) {
                return null;
            }
            return new StoredSession(refreshToken, accessToken, accessExpiresAt);
        } catch (IOException ioException) {
            Client.LOG.warn("[Alumite] Failed to read session store: {}", ioException.getMessage());
            return null;
        }
    }

    void save(String accountKey, String refreshToken, String accessToken, String accessExpiresAt) {
        try {
            Path path = storePath(accountKey);
            Files.createDirectories(path.getParent());
            String content = refreshToken + "\n" + (accessToken != null ? accessToken : "") + "\n" + (accessExpiresAt != null ? accessExpiresAt : "");
            Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ioException) {
            Client.LOG.warn("[Alumite] Failed to save session: {}", ioException.getMessage());
        }
    }

    void clear(String accountKey) {
        try {
            Files.deleteIfExists(storePath(accountKey));
        } catch (IOException ioException) {
            Client.LOG.warn("[Alumite] Failed to clear session: {}", ioException.getMessage());
        }
    }

    private String sanitizeAccountKey(String accountKey) {
        return accountKey.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
