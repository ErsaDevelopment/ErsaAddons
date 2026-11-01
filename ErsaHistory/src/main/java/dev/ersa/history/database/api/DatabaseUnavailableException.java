package dev.ersa.history.database.api;

public final class DatabaseUnavailableException extends RuntimeException {
    public DatabaseUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseUnavailableException(String message) {
        super(message);
    }
}
