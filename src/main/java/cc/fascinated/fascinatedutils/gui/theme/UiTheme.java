package cc.fascinated.fascinatedutils.gui.theme;

/**
 * Semantic color tokens for the Alumite UI. Components read these instead of literals so a theme restyles everything.
 */
public interface UiTheme {
    /**
     * Fill of windows and panels.
     *
     * @return packed ARGB
     */
    int panelFill();

    /**
     * Fill of panel title bars.
     *
     * @return packed ARGB
     */
    int panelHeaderFill();

    /**
     * Outline of windows and panels.
     *
     * @return packed ARGB
     */
    int panelBorder();

    /**
     * Thin divider line between sections.
     *
     * @return packed ARGB
     */
    int divider();

    /**
     * Thickness of divider lines in logical pixels.
     *
     * @return thickness in logical pixels
     */
    int separatorThickness();

    /**
     * Brand accent for text, icons and highlights.
     *
     * @return packed ARGB
     */
    int accent();

    /**
     * Destructive status tone for text and icons.
     *
     * @return packed ARGB
     */
    int danger();

    /**
     * Content color on a danger fill.
     *
     * @return packed ARGB
     */
    int onDanger();

    /**
     * Text and icon colour drawn on top of {@link #successFill()}.
     *
     * @return packed ARGB
     */
    int onSuccess();

    /**
     * Fill of text inputs.
     *
     * @return packed ARGB
     */
    int inputFill();

    /**
     * Border of text inputs at rest.
     *
     * @return packed ARGB
     */
    int inputBorder();

    /**
     * Text caret color.
     *
     * @return packed ARGB
     */
    int caret();

    /**
     * Button fill at rest.
     *
     * @return packed ARGB
     */
    int buttonFill();

    /**
     * Button fill while hovered.
     *
     * @return packed ARGB
     */
    int buttonFillHover();

    /**
     * Button border at rest.
     *
     * @return packed ARGB
     */
    int buttonBorder();

    /**
     * Button border while hovered.
     *
     * @return packed ARGB
     */
    int buttonBorderHover();

    /**
     * Main text color.
     *
     * @return packed ARGB
     */
    int textPrimary();

    /**
     * Secondary text color.
     *
     * @return packed ARGB
     */
    int textMuted();

    /**
     * Tertiary text color for hints and captions.
     *
     * @return packed ARGB
     */
    int textSubtle();

    /**
     * Scrollbar track.
     *
     * @return packed ARGB
     */
    int scrollbarTrack();

    /**
     * Scrollbar thumb.
     *
     * @return packed ARGB
     */
    int scrollbarThumb();

    /**
     * Fill for destructive actions; white text stays legible on it.
     *
     * @return packed ARGB
     */
    int dangerFill();

    /**
     * Hovered variant of {@link #dangerFill()}.
     *
     * @return packed ARGB
     */
    int dangerFillHover();

    /**
     * Fill for confirmations such as an enabled state; white text stays legible on it.
     *
     * @return packed ARGB
     */
    int successFill();

    /**
     * Hovered variant of {@link #successFill()}.
     *
     * @return packed ARGB
     */
    int successFillHover();

    /**
     * Dimming layer behind popups and dialogs.
     *
     * @return packed ARGB
     */
    int popupBackdropFill();



    /**
     * Card and control surface, one step above the panel.
     *
     * @return packed ARGB color
     */
    int surfaceCard();

    /**
     * Raised surface for hovered or elevated controls, one step above the card.
     *
     * @return packed ARGB color
     */
    int surfaceRaised();

    /**
     * Pressed or active surface, one step above raised.
     *
     * @return packed ARGB color
     */
    int surfaceActive();

    /**
     * Hairline border between surfaces.
     *
     * @return packed ARGB color
     */
    int borderSubtle();

    /**
     * Border for interactive controls at rest.
     *
     * @return packed ARGB color
     */
    int borderStrong();

    /**
     * Accent fill while hovered. Keeps white text at 4.5:1 or better.
     *
     * @return packed ARGB color
     */
    int accentHover();

    /**
     * Content color on an accent fill.
     *
     * @return packed ARGB color
     */
    int onAccent();

    /**
     * Keyboard focus ring.
     *
     * @return packed ARGB color
     */
    int focusRing();

    /**
     * Positive status tone for text and icons.
     *
     * @return packed ARGB color
     */
    int success();

    /**
     * Cautionary status tone for text and icons.
     *
     * @return packed ARGB color
     */
    int warning();



    /**
     * Neutral informational status tone for text and icons.
     *
     * @return packed ARGB
     */
    int info();



    /**
     * Background of selected text.
     *
     * @return packed ARGB
     */
    int selection();

    /**
     * Border of an input holding an invalid value.
     *
     * @return packed ARGB
     */
    int inputInvalidBorder();

    /**
     * Outer ring of a picker crosshair; must contrast with any picked color.
     *
     * @return packed ARGB
     */
    int crosshairOuter();

    /**
     * Inner ring of a picker crosshair.
     *
     * @return packed ARGB
     */
    int crosshairInner();

    /**
     * Grey level for a loading spinner dot.
     *
     * @param brightness 0 for the dimmest dot, 1 for the brightest
     * @return packed ARGB
     */
    int spinnerDot(float brightness);
}
