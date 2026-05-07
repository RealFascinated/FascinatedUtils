package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.FRectWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

import java.util.function.BooleanSupplier;

public class SocialLeftPaneWidget {

    public static FWidget build(Props props) {
        return new FWidget() {
            {
                addChild(props.background());
                addChild(props.header());
                addChild(props.tabs());
                addChild(props.list());
                addChild(props.footer());
                addChild(props.userProfileFooter());
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                props.background().layout(measure, lx, ly, lw, lh);
                float cursorY = ly + props.padding();
                float headerHeight = Math.max(56f, props.header().intrinsicHeightForColumn(measure, lw - 2f * props.padding()));
                props.header().layout(measure, lx + props.padding(), cursorY, lw - 2f * props.padding(), headerHeight);
                cursorY += headerHeight + 8f;
                props.tabs().layout(measure, lx, cursorY, lw, props.tabHeight());
                cursorY += props.tabHeight() + 8f;

                float footerHeight = props.footerVisible().getAsBoolean() ? 32f : 0f;
                props.footer().setVisible(props.footerVisible().getAsBoolean());
                if (props.footerVisible().getAsBoolean()) {
                    props.footer().layout(measure, lx + props.padding(), cursorY, lw - 2f * props.padding(), footerHeight);
                    cursorY += footerHeight + 8f;
                }
                else {
                    props.footer().layout(measure, lx + props.padding(), cursorY, lw - 2f * props.padding(), 0f);
                }

                float profileFooterH = 36f;
                float profileFooterY = (float) Math.floor(ly + lh - profileFooterH);
                float listHeight = Math.max(0f, profileFooterY - 8f - cursorY);
                props.list().layout(measure, lx + props.padding(), cursorY, lw - 2f * props.padding(), listHeight);
                props.userProfileFooter().layout(measure, lx + props.padding(), profileFooterY, lw - 2f * props.padding(), profileFooterH);
            }
        };
    }

    public record Props(float padding, float tabHeight, FRectWidget background, FWidget header, FWidget tabs,
                        FWidget list, FWidget footer, BooleanSupplier footerVisible, FWidget userProfileFooter) {}
}
