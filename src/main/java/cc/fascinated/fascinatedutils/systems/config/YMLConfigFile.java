package cc.fascinated.fascinatedutils.systems.config;

import cc.fascinated.fascinatedutils.client.Client;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class YMLConfigFile {
    private final String resourcePath;

    public YMLConfigFile(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public Map<String, Object> loadAsMap() {
        try (InputStream stream = YMLConfigFile.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return Collections.emptyMap();
            }
            Yaml yaml = new Yaml();
            Object loadedRoot = yaml.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            if (!(loadedRoot instanceof Map<?, ?> rootMap)) {
                return Collections.emptyMap();
            }
            Map<String, Object> castedRoot = new HashMap<>();
            for (Map.Entry<?, ?> entry : rootMap.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    castedRoot.put(key, entry.getValue());
                }
            }
            return castedRoot;
        } catch (Exception exception) {
            Client.LOG.warn("Failed to load YML resource {}: {}", resourcePath, exception.toString());
            return Collections.emptyMap();
        }
    }
}
