package cc.fascinated.fascinatedutils.renderer.text;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Routes GUI text through the vanilla {@link net.minecraft.client.gui.Font}.
 */
public class VanillaTextRenderer implements TextRenderer {
    public static final VanillaTextRenderer INSTANCE = new VanillaTextRenderer();

    private VanillaTextRenderer() {
    }

    private net.minecraft.client.gui.Font font() {
        return Minecraft.getInstance().font;
    }

    @Override
    public double getWidth(String text, int length, boolean shadow) {
        if (text.isEmpty()) {
            return 0;
        }
        String slice = length >= text.length() ? text : text.substring(0, length);
        int w = font().width(slice);
        return shadow ? w + 1 : w;
    }

    @Override
    public int getWidth(Component text) {
        return Math.max(1, font().width(text));
    }

    @Override
    public double getHeight(boolean shadow) {
        int h = font().lineHeight;
        return shadow ? h : h - 1;
    }

    @Override
    public void drawString(GuiGraphicsExtractor drawContext, String text, int originX, int originY, int colorArgb, boolean shadow) {
        drawContext.text(font(), text, originX, originY, colorArgb, shadow);
    }

    @Override
    public void drawText(GuiGraphicsExtractor drawContext, Component text, int originX, int originY, int colorArgb, boolean shadow) {
        drawContext.text(font(), text, originX, originY, colorArgb, shadow);
    }
}
