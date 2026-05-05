package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.FRectWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

public class SocialRightPaneWidget {

    public record Props(
            float padding,
            FRectWidget background,
            FWidget header,
            FWidget body,
            FWidget footer
    ) {
    }

    public static FWidget build(Props props) {
        return new FWidget() {
            {
                addChild(props.background());
                addChild(props.header());
                addChild(props.body());
                addChild(props.footer());
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                props.background().layout(measure, lx, ly, lw, lh);
                float headerHeight = 40f;
                float footerHeight = 32f;
                props.header().layout(measure, lx + props.padding(), ly + props.padding(), lw - 2f * props.padding(), headerHeight);
                props.footer().layout(measure, lx + props.padding(), ly + lh - props.padding() - footerHeight, lw - 2f * props.padding(), footerHeight);
                float bodyY = ly + props.padding() + headerHeight + 8f;
                float bodyHeight = Math.max(0f, lh - (props.padding() + headerHeight + 8f) - (props.padding() + footerHeight + props.padding()));
                props.body().layout(measure, lx + props.padding(), bodyY, lw - 2f * props.padding(), bodyHeight);
            }
        };
    }
}
