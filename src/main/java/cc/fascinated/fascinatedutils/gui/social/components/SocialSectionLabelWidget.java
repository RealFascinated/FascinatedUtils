package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

public class SocialSectionLabelWidget extends FWidget {

    private final String text;

    public SocialSectionLabelWidget(String text) {
        this.text = text;
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
        float textY = y() + (h() - graphics.getFontCapHeight()) / 2f;
        graphics.drawText(text, x(), textY, FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
        graphics.drawRect(x(), y() + h() - 1f, w(), 1f, 0x22FFFFFF);
    }
}
