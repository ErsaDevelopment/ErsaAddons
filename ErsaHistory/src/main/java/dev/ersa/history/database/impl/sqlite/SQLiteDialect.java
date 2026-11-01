package dev.ersa.history.database.impl.sqlite;

import dev.ersa.history.database.api.DatabaseDialect;

public final class SQLiteDialect implements DatabaseDialect {
    @Override public String getUuidColumnType() { return "BLOB"; }
    @Override public String getAutoIncrementPrimaryKeySyntax() { return "INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT"; }
    @Override public String getInsertOrIgnoreSyntax(String tableName, String columnName) { return "INSERT OR IGNORE INTO " + tableName + " (" + columnName + ") VALUES (?)"; }
    @Override public String getUniqueConstraintViolationSQLState() { return "23000"; }
    @Override public int getUniqueConstraintViolationErrorCode() { return 19; }
    @Override public String getConnectionInitializationSql() { return "PRAGMA foreign_keys = ON;"; }
    @Override public java.sql.Driver newDriver() { return new org.sqlite.JDBC(); }
}