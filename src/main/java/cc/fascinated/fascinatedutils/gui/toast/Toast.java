package cc.fascinated.fascinatedutils.gui.toast;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;

/**
 * An immutable notification shown in the corner of the screen by {@link ToastManager}.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Accessors(fluent = true)
public class Toast {

    private final String title;
    private final String message;
    private final Type type;
    private final float durationSeconds;

    /**
     * Starts building a toast.
     *
     * @return a new builder; finish it with {@link Builder#success()}, {@link Builder#error()},
     * {@link Builder#warning()} or {@link Builder#info()}
     */
    public static Builder show() {
        return new Builder();
    }

    /**
     * Severity of a toast; selects its icon and accent color.
     */
    public enum Type {
        SUCCESS, ERROR, WARNING, INFO
    }

    /**
     * Fluent builder that enqueues the toast on the terminal call.
     */
    public static class Builder {

        private static final float DEFAULT_DURATION_SECONDS = 6f;

        private String title;
        private String message = "";
        private float durationSeconds = DEFAULT_DURATION_SECONDS;

        private static String defaultTitle(Type type) {
            return switch (type) {
                case SUCCESS -> Component.translatable("alumite.toast.title.success").getString();
                case ERROR -> Component.translatable("alumite.toast.title.error").getString();
                case WARNING -> Component.translatable("alumite.toast.title.warning").getString();
                case INFO -> Component.translatable("alumite.toast.title.info").getString();
            };
        }

        /**
         * Sets the body text.
         *
         * @param message the body text; {@code null} is treated as empty
         * @return this builder
         */
        public Builder message(String message) {
            this.message = message == null ? "" : message;
            return this;
        }

        /**
         * Overrides the default title for the toast's type.
         *
         * @param title the title; {@code null} or blank keeps the default
         * @return this builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets how long the toast stays before fading out.
         *
         * @param durationSeconds seconds on screen; values at or below zero keep the default
         * @return this builder
         */
        public Builder duration(float durationSeconds) {
            if (durationSeconds > 0f) {
                this.durationSeconds = durationSeconds;
            }
            return this;
        }

        /**
         * Enqueues the toast as a success.
         */
        public void success() {
            commit(Type.SUCCESS);
        }

        /**
         * Enqueues the toast as an error.
         */
        public void error() {
            commit(Type.ERROR);
        }

        /**
         * Enqueues the toast as a warning.
         */
        public void warning() {
            commit(Type.WARNING);
        }

        /**
         * Enqueues the toast as an informational message.
         */
        public void info() {
            commit(Type.INFO);
        }

        private void commit(Type type) {
            String resolvedTitle = title == null || title.isBlank() ? defaultTitle(type) : title;
            ToastManager.INSTANCE.add(new Toast(resolvedTitle, message, type, durationSeconds));
        }
    }
}
