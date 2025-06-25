package trade.project.common.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final String errorCode;
    private final int statusCode;

    public ApiException(String message) {
        super(message);
        this.errorCode = "API_ERROR";
        this.statusCode = 500;
    }

    public ApiException(String message, String errorCode, int statusCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "API_ERROR";
        this.statusCode = 500;
    }

    public ApiException(String message, String errorCode, int statusCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }
} 