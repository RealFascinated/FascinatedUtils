package cc.fascinated.fascinatedutils.gui.theme;

import cc.fascinated.fascinatedutils.gui.theme.impl.DefaultUiTheme;

/**
 * Holds the active {@link UiTheme}.
 *
 * <p>The active theme is a plain static written once per frame by the root screen and read while drawing, so it is
 * render-thread only and intentionally not volatile.</p>
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
