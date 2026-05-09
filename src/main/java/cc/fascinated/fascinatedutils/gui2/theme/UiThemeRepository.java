package cc.fascinated.fascinatedutils.gui2.theme;

import cc.fascinated.fascinatedutils.gui2.theme.impl.DefaultUiTheme;

/**
 * Global holder for the active gui2 theme.
 *
 * <p>Call {@link #set(UiTheme)} before rendering to switch themes.
 * Use {@link #get()} anywhere — including compose time — to read the current theme.</p>
 */
public class UiThemeRepository {
    private static UiTheme active = DefaultUiTheme.INSTANCE;

    private UiThemeRepository() {}

    /**
     * Returns the active theme.
     *
     * @return the current theme, never {@code null}
     */
    public static UiTheme get() {
        return active;
    }

    /**
     * Replaces the active theme.
     *
     * @param theme the new theme, must not be {@code null}
     */
    public static void set(UiTheme theme) {
        if (theme == null) {
            throw new IllegalArgumentException("theme must not be null");
        }
        active = theme;
    }
}
