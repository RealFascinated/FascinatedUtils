package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.core.GuiFocusState;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import org.lwjgl.glfw.GLFW;

public class SocialChatComposerWidget {
    public static FWidget build(Props props) {
        SocialAttachButtonWidget attachButton = props.attachButton();
        return new FWidget() {
            {
                if (attachButton != null) {
                    addChild(attachButton);
                }
                addChild(props.input());
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                float attachW = attachButton != null ? 24f : 0f;
                float inputW = lw - attachW;
                float inputH = props.input().intrinsicHeightForColumn(measure, inputW);
                float inputY = ly + (lh - inputH) / 2f;
                float cursorX = lx;
                if (attachButton != null) {
                    attachButton.layout(measure, cursorX, inputY, 20f, inputH);
                    cursorX += attachW;
                }
                props.input().layout(measure, cursorX, inputY, inputW, inputH);
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
                if (keyCode == GLFW.GLFW_KEY_UP && props.input().value().isEmpty() && props.onUpArrow() != null) {
                    props.onUpArrow().run();
                    return true;
                }
                return false;
            }
        };
    }

    public record Props(FOutlinedTextInputWidget input, Runnable onSend, Runnable onUpArrow,
                         SocialAttachButtonWidget attachButton) {
        public Props(FOutlinedTextInputWidget input, Runnable onSend, Runnable onUpArrow) {
            this(input, onSend, onUpArrow, null);
        }
    }
}
