package cc.fascinated.fascinatedutils.gui.theme.impl;

import cc.fascinated.fascinatedutils.gui.theme.UiTheme;

/**
 * The Alumite dark theme. Surfaces are a violet-tinted neutral ramp (hue about 262 degrees, very low chroma) so the
 * purple accent belongs to the same family instead of sitting on top of a blue-grey. Each step is lighter than the
 * last, which carries elevation without borders or shadows. Contrast targets, measured on the panel and card
 * surfaces: primary text 14:1, secondary text 8:1, tertiary text 5:1, accent text 7.5:1, white on the accent fill
 * 5:1, and accent fill against the panel 4:1.
 */
public class DefaultUiTheme implements UiTheme {
    public static final DefaultUiTheme INSTANCE = new DefaultUiTheme();

    private static final int PANEL = 0xF516151F;
    private static final int PANEL_HEADER = 0xFF1A1925;
    private static final int SURFACE_INPUT = 0xFF100F17;
    private static final int SURFACE_CARD = 0xFF1D1C28;
    private static final int SURFACE_RAISED = 0xFF262534;
    private static final int SURFACE_ACTIVE = 0xFF302F42;
    private static final int BORDER_SUBTLE = 0xFF2A2938;
    private static final int BORDER_CONTROL = 0xFF34334A;
    private static final int BORDER_STRONG = 0xFF3A394D;
    private static final int BORDER_HOVER = 0xFF47465F;
    private static final int ACCENT_LIGHT = 0xFFB3A0FF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;
    private static final int DANGER_FILL = 0xFFC73A52;
    private static final int SUCCESS_FILL = 0xFF1F8A55;

    @Override
    public int panelFill() {
        return PANEL;
    }

    @Override
    public int panelHeaderFill() {
        return PANEL_HEADER;
    }

    @Override
    public int panelBorder() {
        return BORDER_SUBTLE;
    }

    @Override
    public int divider() {
        return 0x14FFFFFF;
    }

    @Override
    public int separatorThickness() {
        return 1;
    }

    @Override
    public int accent() {
        return 0xFF7352E8;
    }

    @Override
    public int danger() {
        return 0xFFFF6B80;
    }

    @Override
    public int onDanger() {
        return WHITE;
    }

    @Override
    public int onSuccess() {
        return WHITE;
    }

    @Override
    public int inputFill() {
        return SURFACE_INPUT;
    }

    @Override
    public int inputBorder() {
        return BORDER_CONTROL;
    }

    @Override
    public int caret() {
        return ACCENT_LIGHT;
    }

    @Override
    public int buttonFill() {
        return SURFACE_RAISED;
    }

    @Override
    public int buttonFillHover() {
        return SURFACE_ACTIVE;
    }

    @Override
    public int buttonBorder() {
        return BORDER_CONTROL;
    }

    @Override
    public int buttonBorderHover() {
        return BORDER_HOVER;
    }

    @Override
    public int textPrimary() {
        return 0xFFECEBF5;
    }

    @Override
    public int textMuted() {
        return 0xFFB6B4C8;
    }

    @Override
    public int textSubtle() {
        return 0xFF9896AD;
    }

    @Override
    public int scrollbarTrack() {
        return 0x0DFFFFFF;
    }

    @Override
    public int scrollbarThumb() {
        return 0x4DFFFFFF;
    }

    @Override
    public int dangerFill() {
        return DANGER_FILL;
    }

    @Override
    public int dangerFillHover() {
        return 0xFFCC3E56;
    }

    @Override
    public int successFill() {
        return SUCCESS_FILL;
    }

    @Override
    public int successFillHover() {
        return 0xFF259860;
    }

    @Override
    public int popupBackdropFill() {
        return 0xA6090810;
    }



    @Override
    public int surfaceCard() {
        return SURFACE_CARD;
    }

    @Override
    public int surfaceRaised() {
        return SURFACE_RAISED;
    }

    @Override
    public int surfaceActive() {
        return SURFACE_ACTIVE;
    }

    @Override
    public int borderSubtle() {
        return BORDER_SUBTLE;
    }

    @Override
    public int borderStrong() {
        return BORDER_STRONG;
    }

    @Override
    public int accentHover() {
        return 0xFF7857EC;
    }

    @Override
    public int onAccent() {
        return WHITE;
    }

    @Override
    public int focusRing() {
        return ACCENT_LIGHT;
    }

    @Override
    public int success() {
        return 0xFF5ED39A;
    }

    @Override
    public int warning() {
        return 0xFFFFC857;
    }



    @Override
    public int info() {
        return accent();
    }



    @Override
    public int selection() {
        return 0x55AACCFF;
    }

    @Override
    public int inputInvalidBorder() {
        return danger();
    }

    @Override
    public int crosshairOuter() {
        return BLACK;
    }

    @Override
    public int crosshairInner() {
        return WHITE;
    }

    @Override
    public int spinnerDot(float brightness) {
        int channel = Math.round(Math.max(0f, Math.min(1f, brightness)) * 255);
        return BLACK | (channel << 16) | (channel << 8) | channel;
    }
}
