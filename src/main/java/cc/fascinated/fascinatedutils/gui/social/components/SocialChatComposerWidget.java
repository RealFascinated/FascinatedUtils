package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.core.GuiFocusState;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.FButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import org.lwjgl.glfw.GLFW;

public class SocialChatComposerWidget {
    public record Props(
            FOutlinedTextInputWidget input,
            Runnable onSend
    ) {
    }

    public static FWidget build(Props props) {
        FButtonWidget sendButton = new FButtonWidget(props.onSend(), () -> ">", 26f, 1, 1f, 4f, 1f, 4f, 3f);
        return new FWidget() {
            {
                addChild(props.input());
                addChild(sendButton);
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                float inputW = lw - 30f;
                float inputH = props.input().intrinsicHeightForColumn(measure, inputW);
                float inputY = ly + (lh - inputH) / 2f;
                props.input().layout(measure, lx, inputY, inputW, inputH);
                sendButton.layout(measure, lx + inputW + 4f, ly + (lh - 20f) / 2f, 26f, 20f);
            }

            @Override
            public boolean keyDownCapture(int keyCode, int modifiers) {
                if (GuiFocusState.getFocusedId() != props.input().focusId()) {
                    return false;
                }
                if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                    if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
                        return true;
                    }
                    props.onSend().run();
                    return true;
                }
                return false;
            }
        };
    }
}
