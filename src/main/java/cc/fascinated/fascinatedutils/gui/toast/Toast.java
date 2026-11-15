package cc.fascinated.fascinatedutils.gui.toast;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Accessors(fluent = true)
public class Toast {

    private final String title;
    private final String message;
    private final Type type;
    private final float durationSeconds;

    public static Builder show() {
        return new Builder();
    }

    public enum Type {
        SUCCESS, ERROR, WARNING, INFO
    }

    public static class Builder {

        private static final float DURATION_SECONDS = 3f;

        private String message = "";

        private static String defaultTitle(Type type) {
            return switch (type) {
                case SUCCESS -> "Success";
                case ERROR -> "Error";
                case WARNING -> "Warning";
                case INFO -> "Info";
            };
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public void info() {
            commit(Type.INFO);
        }

        private void commit(Type type) {
            ToastManager.INSTANCE.add(new Toast(defaultTitle(type), message, type, DURATION_SECONDS));
        }
    }
}
