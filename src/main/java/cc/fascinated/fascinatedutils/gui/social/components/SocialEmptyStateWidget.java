package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

public class SocialEmptyStateWidget extends FWidget {

    private final String message;

    public SocialEmptyStateWidget(String message) {
        this.message = message;
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        return measure.getFontCapHeight() + 8f;
    }

    @Override
    public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
        setBounds(lx, ly, lw, lh);
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
        graphics.drawCenteredText(message, x() + w() / 2f, y() + 4f, FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
    }
}
