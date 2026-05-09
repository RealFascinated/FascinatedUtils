package cc.fascinated.fascinatedutils.oldgui.toast;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Accessors(fluent = true)
public class Toast {

    private final String title;
    private final String message;
    private final Type type;
    private final float durationSeconds;
    @Nullable private final String imageId;
    @Nullable private final String imageUrl;
    @Nullable private final Integer imageWidth;
    @Nullable private final Integer imageHeight;

    public static Builder show() {
        return new Builder();
    }

    public enum Type {
        SUCCESS, ERROR, WARNING, INFO
    }

    public static class Builder {

        private String title = null;
        private String message = "";
        private int durationMs = 3000;
        private String imageId = null;
        private String imageUrl = null;
        private Integer imageWidth = null;
        private Integer imageHeight = null;

        private static String defaultTitle(Type type) {
            return switch (type) {
                case SUCCESS -> "Success";
                case ERROR -> "Error";
                case WARNING -> "Warning";
                case INFO -> "Info";
            };
        }

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

        public Builder imageId(String imageId) {
            this.imageId = imageId;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder imageWidth(Integer imageWidth) {
            this.imageWidth = imageWidth;
            return this;
        }

        public Builder imageHeight(Integer imageHeight) {
            this.imageHeight = imageHeight;
            return this;
        }

        public void success() {
            commit(Type.SUCCESS);
        }

        public void error() {
            commit(Type.ERROR);
        }

        public void warning() {
            commit(Type.WARNING);
        }

        public void info() {
            commit(Type.INFO);
        }

        private void commit(Type type) {
            String resolvedTitle = title != null ? title : defaultTitle(type);
            ToastManager.INSTANCE.add(new Toast(resolvedTitle, message, type, durationMs / 1000f, imageId, imageUrl, imageWidth, imageHeight));
        }
    }
}
