package dev.ersa.history.database.api;

import java.util.Locale;

public enum DatabaseType {
    SQLITE,
    POSTGRESQL;

    public static DatabaseType fromString(String value) {
        if (value == null) return SQLITE;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "POSTGRESQL", "POSTGRES", "PG" -> {
                return POSTGRESQL;
            }
            default -> {
                return SQLITE;
            }
        }
    }
}
