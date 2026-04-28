package cc.fascinated.fascinatedutils.api;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

class AlumiteTokenStore {

    private Path storeDirectory() {
        return ModConfig.getDirectory().resolve("alumite_sessions");
    }

    private Path storePath(String accountKey) {
        return storeDirectory().resolve(sanitizeAccountKey(accountKey));
    }

    String load(String accountKey) {
        return readToken(storePath(accountKey));
    }

    private String readToken(Path path) {
        if (!Files.exists(path)) {
            return null;
        }
        try {
            String content = Files.readString(path).strip();
            return content.isEmpty() ? null : content;
        } catch (IOException ioException) {
            Client.LOG.warn("[AlumiteApi] Failed to read session store: {}", ioException.getMessage());
            return null;
        }
    }

    void save(String accountKey, String refreshToken) {
        try {
            Path path = storePath(accountKey);
            Files.createDirectories(path.getParent());
            Files.writeString(path, refreshToken, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ioException) {
            Client.LOG.warn("[AlumiteApi] Failed to save session: {}", ioException.getMessage());
        }
    }

    void clear(String accountKey) {
        try {
            Files.deleteIfExists(storePath(accountKey));
        } catch (IOException ioException) {
            Client.LOG.warn("[AlumiteApi] Failed to clear session: {}", ioException.getMessage());
        }
    }

    private String sanitizeAccountKey(String accountKey) {
        return accountKey.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
