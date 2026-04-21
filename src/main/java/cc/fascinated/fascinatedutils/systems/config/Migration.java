package cc.fascinated.fascinatedutils.systems.config;

import com.google.gson.JsonObject;

public interface Migration {

    /**
     * Returns the config version this migration upgrades from.
     * A migration from version 1 to 2 returns {@code 1}.
     *
     * @return the source version
     */
    int fromVersion();

    /**
     * Transforms a raw config JSON object, producing the next version's representation.
     *
     * @param raw the raw config JSON at {@link #fromVersion()}
     * @return the transformed JSON at {@code fromVersion() + 1}
     */
    JsonObject apply(JsonObject raw);
}
