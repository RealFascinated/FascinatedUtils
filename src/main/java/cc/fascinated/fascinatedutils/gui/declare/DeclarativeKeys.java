package cc.fascinated.fascinatedutils.gui.declare;

import org.jspecify.annotations.Nullable;

public final class DeclarativeKeys {
    private DeclarativeKeys() {
    }

    /**
     * Effective reconciliation key for a slot; positional slots are distinguished by index.
     *
     * @param userKey         explicit key from {@link UiSlot#key}; may be {@code null}
     * @param positionalIndex zero-based child index (used when {@code userKey} is blank)
     * @return non-null key string
     */
    public static String effectiveKey(@Nullable String userKey, int positionalIndex) {
        if (userKey != null && !userKey.isEmpty()) {
            return userKey;
        }
        return "__positional:" + positionalIndex;
    }
}
