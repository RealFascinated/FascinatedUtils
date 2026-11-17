package cc.fascinated.fascinatedutils.gui.components;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Shared translation lookup for UI text that is read every frame. Resolved strings are kept per language and dropped
 * whenever the active language changes, so screens and widgets can ask for a key each frame without caching anything
 * themselves. Render-thread only.
 */
public class UiTranslations {

    public static final UiTranslations INSTANCE = new UiTranslations();

    private final Map<String, String> translated = new HashMap<>();
    private final Map<String, String> upperCased = new HashMap<>();
    private Language language;

    /**
     * Translates a key that takes no arguments.
     *
     * @param key the translation key
     * @return the translated text in the active language
     */
    public String get(String key) {
        invalidateOnLanguageChange();
        return translated.computeIfAbsent(key, translationKey -> Component.translatable(translationKey).getString());
    }

    /**
     * Translates a key that takes no arguments and upper-cases the result.
     *
     * @param key the translation key
     * @return the translated text in the active language, upper-cased
     */
    public String getUpper(String key) {
        invalidateOnLanguageChange();
        return upperCased.computeIfAbsent(key, translationKey -> get(translationKey).toUpperCase(Locale.ROOT));
    }

    private void invalidateOnLanguageChange() {
        Language active = Language.getInstance();
        if (active != language) {
            translated.clear();
            upperCased.clear();
            language = active;
        }
    }
}
