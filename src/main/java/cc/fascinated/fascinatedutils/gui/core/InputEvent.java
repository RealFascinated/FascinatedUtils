package cc.fascinated.fascinatedutils.gui.core;

public sealed interface InputEvent permits InputEvent.MouseMove, InputEvent.MousePress, InputEvent.MouseRelease, InputEvent.MouseScroll, InputEvent.KeyPress, InputEvent.CharType {

    record MouseMove(float positionX, float positionY) implements InputEvent {}

    record MousePress(float positionX, float positionY, int button) implements InputEvent {}

    record MouseRelease(float positionX, float positionY, int button) implements InputEvent {}

    record MouseScroll(float positionX, float positionY, float delta) implements InputEvent {}

    record KeyPress(int keyCode, int scancode, int modifiers) implements InputEvent {}

    record CharType(char character) implements InputEvent {}
}
