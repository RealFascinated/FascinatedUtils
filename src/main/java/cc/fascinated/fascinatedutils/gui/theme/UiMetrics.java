package cc.fascinated.fascinatedutils.gui.theme;

/**
 * Shared spacing, sizing and rounding tokens in logical UI pixels, so every screen uses the same rhythm.
 *
 * <p>Spacing follows a 4px scale. Every interactive control (buttons, inputs, chips, icon buttons) is
 * {@link #CONTROL_HEIGHT} tall, and every rounded surface uses {@link #RADIUS}.</p>
 */
public class UiMetrics {

    /**
     * Tight spacing between closely related items, such as chips in a row or icon and label.
     */
    public static final int SPACE_XS = 4;

    /**
     * Default spacing between siblings and the inner padding of windows and controls.
     */
    public static final int SPACE_SM = 8;

    /**
     * Spacing between groups of content, such as setting sections.
     */
    public static final int SPACE_MD = 12;

    /**
     * Height of every button, input, chip and icon button.
     */
    public static final int CONTROL_HEIGHT = 24;

    /**
     * Height of card title bars and footers; leaves {@link #SPACE_XS} around a control on every side.
     */
    public static final int HEADER_HEIGHT = CONTROL_HEIGHT + 2 * SPACE_XS;

    /**
     * Height of a single list row such as a profile or waypoint.
     */
    public static final int LIST_ROW_HEIGHT = 36;

    /**
     * Corner radius of cards, buttons and inputs.
     */
    public static final int RADIUS = 6;
}
