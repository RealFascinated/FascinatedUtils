package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;

import java.util.function.Supplier;

/**
 * Compact icon-only button primitive. Transparent body, hover swaps the tint; callers configure
 * the icon and tint colors per render via setters so a single instance can be reused.
 */
public class FIconActionButton extends FButtonWidget {
    private Supplier<ModUiTextures> iconSupplier = () -> ModUiTextures.EDIT;
    private int normalTintArgb;
    private int hoverTintArgb;

    public FIconActionButton() {
        super(() -> {}, () -> "", 20f, 1, 1f, 3f, 1f, 3f);
    }

    public void setIcon(ModUiTextures icon) {
        this.iconSupplier = () -> icon;
    }

    public void setIconSupplier(Supplier<ModUiTextures> supplier) {
        this.iconSupplier = supplier == null ? () -> ModUiTextures.EDIT : supplier;
    }

    public void setTints(int normalArgb, int hoverArgb) {
        this.normalTintArgb = normalArgb;
        this.hoverTintArgb = hoverArgb;
    }

    @Override
    protected int resolveButtonFillColorArgb(boolean hovered) {
        return 0x00000000;
    }

    @Override
    protected int resolveButtonBorderColorArgb(boolean hovered) {
        return 0x00000000;
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        int tintArgb = hovered ? hoverTintArgb : normalTintArgb;
        float iconSize = Math.min(w(), h()) - 4f;
        float iconX = x() + (w() - iconSize) / 2f;
        float iconY = y() + (h() - iconSize) / 2f;
        graphics.drawTexture(iconSupplier.get().getId(), iconX, iconY, iconSize, iconSize, tintArgb);
    }
}
