package cc.fascinated.fascinatedutils.gui2.core;

/**
 * Pointer and keyboard input events routed through the gui2 input router.
 */
public interface UiEvent {

    class PointerMove implements UiEvent {
        private final float pointerX;
        private final float pointerY;

        public PointerMove(float pointerX, float pointerY) {
            this.pointerX = pointerX;
            this.pointerY = pointerY;
        }

        public float pointerX() {
            return pointerX;
        }

        public float pointerY() {
            return pointerY;
        }
    }

    class PointerPress implements UiEvent {
        private final float pointerX;
        private final float pointerY;
        private final int button;

        public PointerPress(float pointerX, float pointerY, int button) {
            this.pointerX = pointerX;
            this.pointerY = pointerY;
            this.button = button;
        }

        public float pointerX() {
            return pointerX;
        }

        public float pointerY() {
            return pointerY;
        }

        public int button() {
            return button;
        }
    }

    class PointerRelease implements UiEvent {
        private final float pointerX;
        private final float pointerY;
        private final int button;

        public PointerRelease(float pointerX, float pointerY, int button) {
            this.pointerX = pointerX;
            this.pointerY = pointerY;
            this.button = button;
        }

        public float pointerX() {
            return pointerX;
        }

        public float pointerY() {
            return pointerY;
        }

        public int button() {
            return button;
        }
    }

    class PointerScroll implements UiEvent {
        private final float pointerX;
        private final float pointerY;
        private final float delta;

        public PointerScroll(float pointerX, float pointerY, float delta) {
            this.pointerX = pointerX;
            this.pointerY = pointerY;
            this.delta = delta;
        }

        public float pointerX() {
            return pointerX;
        }

        public float pointerY() {
            return pointerY;
        }

        public float delta() {
            return delta;
        }
    }

    class KeyPress implements UiEvent {
        private final int keyCode;
        private final int modifiers;

        public KeyPress(int keyCode, int modifiers) {
            this.keyCode = keyCode;
            this.modifiers = modifiers;
        }

        public int keyCode() {
            return keyCode;
        }

        public int modifiers() {
            return modifiers;
        }
    }

    class CharType implements UiEvent {
        private final char character;

        public CharType(char character) {
            this.character = character;
        }

        public char character() {
            return character;
        }
    }
}
