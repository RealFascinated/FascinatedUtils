package cc.fascinated.fascinatedutils.gui.input;

import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import org.lwjgl.glfw.GLFW;

public class UiCursorController {
    private static long textCursorHandle;
    private static long handCursorHandle;
    private static long notAllowedCursorHandle;
    private static UiPointerCursor currentCursor = UiPointerCursor.DEFAULT;

    public static void apply(long windowHandle, UiPointerCursor cursor) {
        if (windowHandle == 0L || cursor == null || cursor == currentCursor) {
            return;
        }
        long cursorHandle = switch (cursor) {
            case DEFAULT -> 0L;
            case TEXT -> getTextCursorHandle();
            case HAND -> getHandCursorHandle();
            case NOT_ALLOWED -> getNotAllowedCursorHandle();
        };
        GLFW.glfwSetCursor(windowHandle, cursorHandle);
        currentCursor = cursor;
    }

    private static long getTextCursorHandle() {
        if (textCursorHandle == 0L) {
            textCursorHandle = GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR);
        }
        return textCursorHandle;
    }

    private static long getHandCursorHandle() {
        if (handCursorHandle == 0L) {
            handCursorHandle = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
        }
        return handCursorHandle;
    }

    private static long getNotAllowedCursorHandle() {
        if (notAllowedCursorHandle == 0L) {
            notAllowedCursorHandle = GLFW.glfwCreateStandardCursor(GLFW.GLFW_NOT_ALLOWED_CURSOR);
        }
        return notAllowedCursorHandle;
    }
}
