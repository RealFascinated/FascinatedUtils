package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.network.chat.Component;

public class SocialNoChannelSelectedWidget {
    public static FWidget build() {
        return new FWidget() {
            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                String message = Component.translatable("fascinatedutils.social.dm.select_channel").getString();
                graphics.drawCenteredText(message, x() + w() / 2f, y() + (h() - graphics.getFontCapHeight()) / 2f, FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
            }
        };
    }
}