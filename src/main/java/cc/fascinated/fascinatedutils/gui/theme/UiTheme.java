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

    int rowHoverFill();

    int rowSelectedFill();

    int tabActiveFill();

    int tabHoverFill();

    int inputFill();

    int inputBorder();

    int inputPlaceholderFocused();

    int caret();

    int avatarFallbackFill();

    int avatarRing();

    int buttonFill();

    int buttonFillHover();

    int buttonBorder();

    int buttonBorderHover();

    int buttonBorderFocus();

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

    int attachmentTint();

    int attachmentPlaceholderFill();

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
     * Accent fill while pressed.
     *
     * @return packed ARGB color
     */
    int accentPressed();

    /**
     * Translucent accent tint for selected rows and active tabs.
     *
     * @return packed ARGB color
     */
    int accentSubtle();

    /**
     * Accent tone for text and icons on dark surfaces. Meets 7:1 on the panel.
     *
     * @return packed ARGB color
     */
    int accentText();

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
     * Warning status tone for text and icons.
     *
     * @return packed ARGB color
     */
    int warning();

    /**
     * Danger tone for text and icons on dark surfaces. Meets 6.5:1 on the card surface.
     *
     * @return packed ARGB color
     */
    int dangerText();
}
