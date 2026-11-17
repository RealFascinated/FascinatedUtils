package cc.fascinated.fascinatedutils.gui.node.control;

import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import cc.fascinated.fascinatedutils.gui.core.input.UiKeys;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/**
 * A button that rebinds a key mapping. While listening, the next key or non-primary mouse button becomes the
 * binding, Delete or Backspace clears it and Escape cancels.
 */
public class KeybindNode extends ButtonNode {
    private static final int NO_KEY = GLFW.GLFW_KEY_UNKNOWN;

    private final KeybindSetting keybindSetting;
    private boolean listening;
    // The key that started listening; it is ignored until another key arrives so held-key repeats do not bind it.
    private int startedByKey = NO_KEY;

    public KeybindNode(KeybindSetting keybindSetting) {
        super();
        this.keybindSetting = keybindSetting;
        setLabel(() -> listening ? "..." : keybindSetting.currentBindingLabel());
        setDisabled(keybindSetting::isLocked);
        setOnPress(() -> {
            listening = !listening;
            startedByKey = NO_KEY;
        });
    }

    @Override
    protected void inheritState(UiNode previous) {
        super.inheritState(previous);
        KeybindNode previousKeybind = (KeybindNode) previous;
        listening = previousKeybind.listening;
        startedByKey = previousKeybind.startedByKey;
    }

    @Override
    public boolean onKeyPress(int keyCode, int modifiers) {
        if (!listening) {
            boolean activated = super.onKeyPress(keyCode, modifiers);
            if (activated && listening && UiKeys.isActivationKey(keyCode)) {
                startedByKey = keyCode;
            }
            return activated;
        }
        if (keyCode == startedByKey) {
            return true;
        }
        listening = false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            keybindSetting.applyBinding(InputConstants.UNKNOWN);
            return true;
        }
        keybindSetting.applyBinding(InputConstants.getKey(new KeyEvent(keyCode, 0, modifiers)));
        return true;
    }

    @Override
    public boolean onPointerPress(float pointerX, float pointerY, int button) {
        if (!listening || button == 0) {
            return super.onPointerPress(pointerX, pointerY, button);
        }
        listening = false;
        keybindSetting.applyBinding(InputConstants.Type.MOUSE.getOrCreate(button));
        return true;
    }

    @Override
    public void onFocusLost() {
        listening = false;
        super.onFocusLost();
    }
}
