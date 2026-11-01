package dev.ersa.history.database.api;

public interface DatabaseDialect {
    String getUuidColumnType();
    String getAutoIncrementPrimaryKeySyntax();
    String getInsertOrIgnoreSyntax(String tableName, String columnName);
    String getUniqueConstraintViolationSQLState();
    int getUniqueConstraintViolationErrorCode();
    java.sql.Driver newDriver();

    default String quoteIdentifier(String identifier) {
        return "\"" + identifier + "\"";
    }

    default String getConnectionInitializationSql() {
        return null;
    }
}
