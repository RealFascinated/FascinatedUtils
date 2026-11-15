package cc.fascinated.fascinatedutils.api;

import lombok.Getter;

public class AlumiteApiException extends RuntimeException {
    @Getter
    private final Errors error;
    /**
     * HTTP status of the failed response, or {@code 0} when no response was received (network failure).
     */
    @Getter
    private final int statusCode;

    public AlumiteApiException(Errors error, String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.error = error;
        this.statusCode = statusCode;
    }

    /**
     * Whether retrying the same request later could succeed.
     *
     * @return true for network failures, rate limiting and server errors
     */
    public boolean isTransient() {
        return statusCode == 0 || statusCode == 429 || statusCode >= 500;
    }
}
