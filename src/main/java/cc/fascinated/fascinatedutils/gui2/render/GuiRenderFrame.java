package cc.fascinated.fascinatedutils.gui2.render;

import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import cc.fascinated.fascinatedutils.oldgui.GuiTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Preferred gui2 renderer type name to avoid collisions with oldgui GuiRenderer imports.
 */
public class GuiRenderFrame extends GuiRenderer {
    public GuiRenderFrame(GuiGraphicsExtractor drawContext, GuiTheme guiTheme, UiTheme uiTheme) {
        super(drawContext, guiTheme, uiTheme);
    }
}
