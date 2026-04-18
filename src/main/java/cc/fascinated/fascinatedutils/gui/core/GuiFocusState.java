package cc.fascinated.fascinatedutils.gui.core;

public final class GuiFocusState {
    private static volatile int focusedId = UiFocusIds.NO_FOCUS_ID;

    private GuiFocusState() {
    }

    public static int getFocusedId() {
        return focusedId;
    }

    public static void setFocusedId(int id) {
        focusedId = id;
    }
}
