package cc.fascinated.fascinatedutils.gui.input;

public class MouseButtons {
    public static final int LEFT = 1;
    public static final int RIGHT = 1 << 1;
    public static final int MIDDLE = 1 << 2;

    public static int bitForGlfwButton(int glfwButton) {
        if (glfwButton < 0 || glfwButton > 7) {
            return 0;
        }
        return 1 << glfwButton;
    }
}
