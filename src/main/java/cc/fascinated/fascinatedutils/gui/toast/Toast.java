package cc.fascinated.fascinatedutils.gui.toast;

public class Toast {

    public enum Type {
        SUCCESS, ERROR, WARNING, INFO
    }

    private final String message;
    private final Type type;
    private final float durationSeconds;

    Toast(String message, Type type, float durationSeconds) {
        this.message = message;
        this.type = type;
        this.durationSeconds = durationSeconds;
    }

    public String message() { return message; }
    public Type type() { return type; }
    public float durationSeconds() { return durationSeconds; }

    public static Builder show() {
        return new Builder();
    }

    public static class Builder {

        private String message = "";
        private int durationMs = 3000;

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
            ToastManager.INSTANCE.add(new Toast(message, type, durationMs / 1000f));
        }
    }
}
