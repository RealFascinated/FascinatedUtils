package cc.fascinated.fascinatedutils.systems.config.serialization;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.util.Optional;

public class JsonObjectWrapper {
    private final JsonObject delegate;

    /**
     * Wraps a Gson JSON object for typed reads.
     */
    public JsonObjectWrapper(JsonObject delegate) {
        this.delegate = delegate;
    }

    /**
     * Exposes the backing JSON object for advanced Gson use.
     */
    public JsonObject unwrap() {
        return delegate;
    }

    /**
     * Whether {@code member} exists and is not JSON null.
     */
    public boolean has(String member) {
        return delegate.has(member) && !delegate.get(member).isJsonNull();
    }

    /**
     * Returns the raw member value without null checks.
     */
    public JsonElement getMember(String member) {
        return delegate.get(member);
    }

    /**
     * Deserializes a member using a {@link TypeToken} for generic-safe Gson reads.
     */
    public <V> Optional<V> get(String member, TypeToken<V> type, Gson gson) {
        if (!has(member)) {
            return Optional.empty();
        }
        return Optional.of(gson.fromJson(delegate.get(member), type.getType()));
    }
}
