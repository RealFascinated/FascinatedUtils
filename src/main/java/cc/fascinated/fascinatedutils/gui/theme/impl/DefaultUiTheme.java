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

    @Override
    public int panelFill() {
        return 0xF516151F;
    }

    @Override
    public int panelHeaderFill() {
        return 0xFF1A1925;
    }

    @Override
    public int panelBorder() {
        return 0xFF2A2938;
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
        return 0xFFFFFFFF;
    }

    @Override
    public int onSuccess() {
        return 0xFFFFFFFF;
    }





    @Override
    public int inputFill() {
        return 0xFF100F17;
    }

    @Override
    public int inputBorder() {
        return 0xFF34334A;
    }


    @Override
    public int caret() {
        return 0xFFB3A0FF;
    }



    @Override
    public int buttonFill() {
        return 0xFF262534;
    }

    @Override
    public int buttonFillHover() {
        return 0xFF302F42;
    }

    @Override
    public int buttonBorder() {
        return 0xFF34334A;
    }

    @Override
    public int buttonBorderHover() {
        return 0xFF47465F;
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
        return 0xFFC73A52;
    }

    @Override
    public int successFill() {
        return 0xFF1F8A55;
    }

    @Override
    public int successFillHover() {
        return 0xFF259860;
    }

    @Override
    public int dangerFillHover() {
        return 0xFFCC3E56;
    }



    @Override
    public int popupBackdropFill() {
        return 0xA6090810;
    }

    @Override
    public int contextMenuFill() {
        return 0xFF1D1C28;
    }

    @Override
    public int contextMenuBorder() {
        return 0xFF34334A;
    }

    @Override
    public int surfaceCard() {
        return 0xFF1D1C28;
    }

    @Override
    public int surfaceRaised() {
        return 0xFF262534;
    }

    @Override
    public int surfaceActive() {
        return 0xFF302F42;
    }

    @Override
    public int borderSubtle() {
        return 0xFF2A2938;
    }

    @Override
    public int borderStrong() {
        return 0xFF3A394D;
    }

    @Override
    public int accentHover() {
        return 0xFF7857EC;
    }




    @Override
    public int onAccent() {
        return 0xFFFFFFFF;
    }

    @Override
    public int focusRing() {
        return 0xFFB3A0FF;
    }

    @Override
    public int success() {
        return 0xFF5ED39A;
    }

    @Override
    public int warning() {
        return 0xFFFFC857;
    }
}
