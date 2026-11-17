package cc.fascinated.fascinatedutils.gui.core.input;

public sealed interface UiEvent {

    record PointerMove(float pointerX, float pointerY) implements UiEvent {}

    record PointerPress(float pointerX, float pointerY, int button) implements UiEvent {}

    record PointerRelease(float pointerX, float pointerY, int button) implements UiEvent {}

    record PointerScroll(float pointerX, float pointerY, float delta) implements UiEvent {}

    record KeyPress(int keyCode, int modifiers) implements UiEvent {}

    /**
     * A typed character.
     *
     * @param codePoint the Unicode code point, which may lie outside the basic multilingual plane
     */
    record CharType(int codePoint) implements UiEvent {}
}
