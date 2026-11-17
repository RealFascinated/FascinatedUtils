package cc.fascinated.fascinatedutils.gui.core.input;

import lombok.experimental.UtilityClass;
import org.lwjgl.glfw.GLFW;

@UtilityClass
public class UiKeys {

    /**
     * Checks whether a key press should activate a focused control.
     *
     * @param keyCode the GLFW key code of the press
     * @return true for Enter, keypad Enter or Space
     */
    public static boolean isActivationKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE;
    }
}
