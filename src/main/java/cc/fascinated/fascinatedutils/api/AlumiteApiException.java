package cc.fascinated.fascinatedutils.api;

public class AlumiteApiException extends RuntimeException {
    private final Errors error;

    public AlumiteApiException(Errors error, String message) {
        super(message);
        this.error = error;
    }

    public Errors getError() {
        return error;
    }

    public String getDisplayText() {
        return getMessage();
    }
}