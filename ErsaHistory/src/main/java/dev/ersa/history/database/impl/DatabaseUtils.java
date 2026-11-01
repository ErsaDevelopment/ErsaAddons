package dev.ersa.history.database.impl;

import dev.ersa.history.database.api.DatabaseDialect;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class DatabaseUtils {
    private DatabaseUtils() {}

    public static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static UUID bytesToUuid(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            throw new IllegalArgumentException("UUID bytes must be 16 bytes, got " + (bytes == null ? "null" : bytes.length));
        }
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }

    public static long getOrCreateId(Connection connection, DatabaseDialect dialect, String tableName, String stringColumnName, String value) throws SQLException {
        String insertSql = dialect.getInsertOrIgnoreSyntax(tableName, stringColumnName);
        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
            insert.setString(1, value);
            insert.executeUpdate();
        }
        try (PreparedStatement select = connection.prepareStatement("SELECT id FROM " + tableName + " WHERE " + stringColumnName + " = ?")) {
            select.setString(1, value);
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
                throw new SQLException("Failed to fetch id for '" + value + "' from " + tableName);
            }
        }
    }
}