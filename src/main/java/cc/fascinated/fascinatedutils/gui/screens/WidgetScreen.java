package cc.fascinated.fascinatedutils.gui.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Widget screen: primary paint is {@link #renderCustom}, invoked by the game renderer mixin after the vanilla GUI
 * has rendered.
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
     * @param mouseX      vanilla GUI-scaled mouse X as passed by the caller; screens that need a pointer position
     *                    read it from {@code UiScale} instead
     * @param mouseY      vanilla GUI-scaled mouse Y as passed by the caller; see {@code mouseX}
     * @param delta       tick delta in ticks
     */
    public abstract void renderCustom(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta);
}
