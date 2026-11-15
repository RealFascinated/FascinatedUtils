package cc.fascinated.fascinatedutils.renderer.text;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Routes GUI text through the vanilla {@link Font}.
 */
public class VanillaTextRenderer implements TextRenderer {
    public static final VanillaTextRenderer INSTANCE = new VanillaTextRenderer();

    @Override
    public int getWidth(Component text) {
        return Math.max(1, font().width(text));
    }

    @Override
    public int getWidth(FormattedCharSequence text) {
        return Math.max(1, font().width(text));
    }

    @Override
    public double getHeight(boolean shadow) {
        return font().lineHeight;
    }

    @Override
    public void drawText(GuiGraphicsExtractor drawContext, FormattedCharSequence text, int originX, int originY, int colorArgb, boolean shadow) {
        drawContext.text(font(), text, originX, originY + 1, colorArgb, shadow);
    }

    @Override
    public void drawText(GuiGraphicsExtractor drawContext, Component text, int originX, int originY, int colorArgb, boolean shadow) {
        drawContext.text(font(), text, originX, originY + 1, colorArgb, shadow);
    }

    private Font font() {
        return Minecraft.getInstance().font;
    }
}
