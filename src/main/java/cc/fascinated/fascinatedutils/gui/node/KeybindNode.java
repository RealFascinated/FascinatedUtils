package cc.fascinated.fascinatedutils.gui.node;

import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

public class KeybindNode extends ButtonNode {

    private final KeybindSetting keybindSetting;
    private boolean listening;

    public KeybindNode(KeybindSetting keybindSetting) {
        super();
        this.keybindSetting = keybindSetting;
        setRounded(true);
        setLabel(() -> listening ? "..." : keybindSetting.currentBindingLabel());
        setDisabled(keybindSetting::isLocked);
        setOnPress(() -> listening = !listening);
    }

    @Override
    public boolean capturesKeyPress(int keyCode, int modifiers) {
        return listening;
    }

    @Override
    public boolean onKeyPress(int keyCode, int modifiers) {
        if (!listening) {
            return super.onKeyPress(keyCode, modifiers);
        }
        listening = false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return true;
        }
        keybindSetting.applyBinding(InputConstants.getKey(new KeyEvent(keyCode, 0, modifiers)));
        return true;
    }

    @Override
    public void onFocusLost() {
        listening = false;
        super.onFocusLost();
    }
}
