package dev.ersa.history.database.impl.postgres;

import dev.ersa.history.database.api.DatabaseDialect;

public final class PostgresDialect implements DatabaseDialect {
    @Override public String getUuidColumnType() { return "BYTEA"; }
    @Override public String getAutoIncrementPrimaryKeySyntax() { return "SERIAL NOT NULL PRIMARY KEY"; }
    @Override public String getInsertOrIgnoreSyntax(String tableName, String columnName) {
        return "INSERT INTO " + tableName + " (" + columnName + ") VALUES (?) ON CONFLICT (" + columnName + ") DO NOTHING";
    }
    @Override public String getUniqueConstraintViolationSQLState() { return "23505"; }
    @Override public int getUniqueConstraintViolationErrorCode() { return 0; }
    @Override public java.sql.Driver newDriver() { return new org.postgresql.Driver(); }
}
