package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.widgets.FRectWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

public class SocialRightPaneWidget {

    public static FWidget build(Props props) {
        return new FWidget() {
            final FWidget separator = new FWidget() {
                @Override
                public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                    setBounds(lx, ly, lw, lh);
                }

                @Override
                protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                    graphics.drawRect(x(), y(), w(), 1f, UITheme.COLOR_BORDER);
                }
            };

            {
                addChild(props.background());
                addChild(props.header());
                addChild(separator);
                addChild(props.body());
                addChild(props.footer());
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                props.background().layout(measure, lx, ly, lw, lh);
                float headerHeight = 30f;
                float footerHeight = 32f;
                props.header().layout(measure, lx + props.padding(), ly, lw - 2f * props.padding(), props.padding() + headerHeight);
                float separatorY = ly + props.padding() + headerHeight;
                separator.layout(measure, lx, separatorY, lw, 1f);
                props.footer().layout(measure, lx + props.padding(), ly + lh - props.padding() - footerHeight, lw - 2f * props.padding(), footerHeight);
                float bodyY = ly + props.padding() + headerHeight + 8f;
                float bodyHeight = Math.max(0f, lh - (props.padding() + headerHeight + 8f) - (props.padding() + footerHeight + props.padding()));
                props.body().layout(measure, lx + props.padding(), bodyY, lw - 2f * props.padding(), bodyHeight);
            }
        };
    }

    public record Props(float padding, FRectWidget background, FWidget header, FWidget body, FWidget footer) {}
}
