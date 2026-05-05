package cc.fascinated.fascinatedutils.gui.toast;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Accessors(fluent = true)
public class Toast {

    public enum Type {
        SUCCESS, ERROR, WARNING, INFO
    }

    private final String title;
    private final String message;
    private final Type type;
    private final float durationSeconds;

    public static Builder show() {
        return new Builder();
    }

    public static class Builder {

        private String title = null;
        private String message = "";
        private int durationMs = 3000;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder duration(int durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public void success() { commit(Type.SUCCESS); }
        public void error()   { commit(Type.ERROR); }
        public void warning() { commit(Type.WARNING); }
        public void info()    { commit(Type.INFO); }

        private void commit(Type type) {
            String resolvedTitle = title != null ? title : defaultTitle(type);
            ToastManager.INSTANCE.add(new Toast(resolvedTitle, message, type, durationMs / 1000f));
        }

        private static String defaultTitle(Type type) {
            return switch (type) {
                case SUCCESS -> "Success";
                case ERROR   -> "Error";
                case WARNING -> "Warning";
                case INFO    -> "Info";
            };
        }
    }
}
