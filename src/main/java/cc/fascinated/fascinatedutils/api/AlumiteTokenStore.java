package cc.fascinated.fascinatedutils.api;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

class AlumiteTokenStore {

    private Path storePath() {
        return ModConfig.getDirectory().resolve("alumite_session");
    }

    String load() {
        Path path = storePath();
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

    void save(String refreshToken) {
        try {
            Path path = storePath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, refreshToken, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ioException) {
            Client.LOG.warn("[AlumiteApi] Failed to save session: {}", ioException.getMessage());
        }
    }

    void clear() {
        try {
            Files.deleteIfExists(storePath());
        } catch (IOException ioException) {
            Client.LOG.warn("[AlumiteApi] Failed to clear session: {}", ioException.getMessage());
        }
    }
}
