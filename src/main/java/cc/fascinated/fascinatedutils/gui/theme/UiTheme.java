package cc.fascinated.fascinatedutils.gui.theme;

public interface UiTheme {
    int panelFill();

    int panelHeaderFill();

    int panelBorder();

    int divider();

    int separatorThickness();

    int accent();

    int danger();

    int onDanger();

    /**
     * Text and icon colour drawn on top of {@link #successFill()}.
     *
     * @return packed ARGB
     */
    int onSuccess();

    int inputFill();

    int inputBorder();

    int caret();

    int buttonFill();

    int buttonFillHover();

    int buttonBorder();

    int buttonBorderHover();

    int textPrimary();

    int textMuted();

    int textSubtle();

    int scrollbarTrack();

    int scrollbarThumb();

    int dangerFill();

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

    int popupBackdropFill();

    int contextMenuFill();

    int contextMenuBorder();

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
}
