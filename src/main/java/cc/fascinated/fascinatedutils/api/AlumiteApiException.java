package cc.fascinated.fascinatedutils.api;

import lombok.Getter;

public class AlumiteApiException extends RuntimeException {
    @Getter
    private final Errors error;

    public AlumiteApiException(Errors error, String message) {
        super(message);
        this.error = error;
    }

    public String getDisplayText() {
        return getMessage();
    }
}