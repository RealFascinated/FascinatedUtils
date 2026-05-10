package cc.fascinated.fascinatedutils.gui2.theme.impl;

import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;

public class DefaultUiTheme implements UiTheme {
    public static final DefaultUiTheme INSTANCE = new DefaultUiTheme();

    @Override
    public int panelFill() {
        return 0xCC1C1F26;
    }

    @Override
    public int panelBorder() {
        return 0xFF3E4453;
    }

    @Override
    public int divider() {
        return 0x22FFFFFF;
    }

    @Override
    public int separatorThickness() {
        return 1;
    }

    @Override
    public int accent() {
        return 0xFF7C5CBF;
    }

    @Override
    public int danger() {
        return 0xFFE05555;
    }

    @Override
    public int onDanger() {
        return 0xFFFFFFFF;
    }

    @Override
    public int rowHoverFill() {
        return 0x22FFFFFF;
    }

    @Override
    public int rowSelectedFill() {
        return 0x334960C8;
    }

    @Override
    public int tabActiveFill() {
        return 0x44FFFFFF;
    }

    @Override
    public int tabHoverFill() {
        return 0x18FFFFFF;
    }

    @Override
    public int inputFill() {
        return 0xFF1A1E2A;
    }

    @Override
    public int inputBorder() {
        return 0xFF3C4357;
    }

    @Override
    public int inputPlaceholderFocused() {
        return 0x44FFFFFF;
    }

    @Override
    public int caret() {
        return 0xFFCCCCCC;
    }

    @Override
    public int avatarFallbackFill() {
        return 0xFF3B445A;
    }

    @Override
    public int avatarRing() {
        return 0xFF1A1E24;
    }

    @Override
    public int buttonFill() {
        return 0xFF353B49;
    }

    @Override
    public int buttonFillHover() {
        return 0xFF4B5366;
    }

    @Override
    public int buttonBorder() {
        return 0xFF59657D;
    }

    @Override
    public int buttonBorderHover() {
        return 0xFF96A1BA;
    }

    @Override
    public int buttonBorderFocus() {
        return 0xFFB7C5E6;
    }

    @Override
    public int textPrimary() {
        return 0xFFF3F5FA;
    }

    @Override
    public int textMuted() {
        return 0xFF8B93A5;
    }

    @Override
    public int textSubtle() {
        return 0x88FFFFFF;
    }

    @Override
    public int scrollbarTrack() {
        return 0x25FFFFFF;
    }

    @Override
    public int scrollbarThumb() {
        return 0x80FFFFFF;
    }

    @Override
    public int dangerFill() {
        return 0x44E05555;
    }

    @Override
    public int dangerFillHover() {
        return 0x66E05555;
    }

    @Override
    public int attachmentTint() {
        return 0xFFFFFFFF;
    }

    @Override
    public int attachmentPlaceholderFill() {
        return 0x33FFFFFF;
    }

    @Override
    public int popupBackdropFill() {
        return 0x80000000;
    }

    @Override
    public int contextMenuFill() {
        return 0xFF1C1F26;
    }

    @Override
    public int contextMenuBorder() {
        return 0xFF3E4453;
    }
}
