package cc.fascinated.fascinatedutils.gui.screens;

import cc.fascinated.fascinatedutils.mixin.GameRendererMixin;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Widget screen: primary paint is {@link #renderCustom}, invoked from
 * {@link GameRendererMixin} after vanilla GUI render.
 */
public abstract class WidgetScreen extends Screen {

    protected WidgetScreen(Component title) {
        super(title);
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    /**
     * Primary paint entry; called with cleared GUI state and framebuffer-space projection (see mixin).
     *
     * @param drawContext draw context for this pass
     * @param mouseX      scaled mouse X (see getScaledX, then multiplied by scale in screen code if needed)
     * @param mouseY      scaled mouse Y
     * @param delta       tick delta
     */
    public abstract void renderCustom(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta);
}
