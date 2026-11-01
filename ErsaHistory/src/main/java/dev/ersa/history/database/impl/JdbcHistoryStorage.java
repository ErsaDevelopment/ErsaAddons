package dev.ersa.history.database.impl;

import dev.ersa.history.database.api.DatabaseDialect;
import dev.ersa.history.database.api.DatabaseUnavailableException;
import dev.ersa.history.database.api.HistoryStorage;
import dev.ersa.history.model.ViolationRecord;
import org.bukkit.plugin.Plugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class JdbcHistoryStorage implements HistoryStorage {
    private static final Set<String> REGISTERED_DRIVERS = ConcurrentHashMap.newKeySet();

    private final Logger logger;
    private final DatabaseDialect dialect;
    private final String jdbcUrl;
    private final Properties connectionProperties;
    private Connection openConnection;

    public JdbcHistoryStorage(Plugin plugin, DatabaseDialect dialect, String jdbcUrl, Properties connectionProperties) {
        this.logger = plugin.getLogger();
        this.dialect = dialect;
        this.jdbcUrl = jdbcUrl;
        this.connectionProperties = connectionProperties;
    }

    @Override
    public synchronized void connect() throws SQLException {
        closeQuietly();

        String initSql = dialect.getConnectionInitializationSql();
        if (initSql != null) {
            try (Connection c = getConnection(); Statement stmt = c.createStatement()) {
                stmt.execute(initSql);
            }
        }

        String pk = dialect.getAutoIncrementPrimaryKeySyntax();
        String uuidType = dialect.getUuidColumnType();

        try (Connection c = getConnection()) {
            createLookupTable(c, DatabaseConstants.SERVERS_TABLE, DatabaseConstants.SERVERS_STRING_COLUMN, pk);
            createLookupTable(c, DatabaseConstants.CHECK_NAMES_TABLE, DatabaseConstants.CHECK_NAMES_STRING_COLUMN, pk);
            createLookupTable(c, DatabaseConstants.CLIENT_BRANDS_TABLE, DatabaseConstants.CLIENT_BRANDS_STRING_COLUMN, pk);
            createLookupTable(c, DatabaseConstants.CLIENT_VERSIONS_TABLE, DatabaseConstants.CLIENT_VERSIONS_STRING_COLUMN, pk);

            try (PreparedStatement ps = c.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + q(DatabaseConstants.VIOLATIONS_TABLE) + "(" +
                            q(DatabaseConstants.VIOLATIONS_ID_COLUMN) + " " + pk + ", " +
                            q(DatabaseConstants.VIOLATIONS_UUID_COLUMN) + " " + uuidType + " NOT NULL, " +
                            q(DatabaseConstants.VIOLATIONS_SERVER_ID_COLUMN) + " INTEGER NOT NULL, " +
                            q(DatabaseConstants.VIOLATIONS_CHECK_NAME_ID_COLUMN) + " INTEGER NOT NULL, " +
                            q(DatabaseConstants.VIOLATIONS_CLIENT_BRAND_ID_COLUMN) + " INTEGER NOT NULL, " +
                            q(DatabaseConstants.VIOLATIONS_CLIENT_VERSION_ID_COLUMN) + " INTEGER NOT NULL, " +
                            q(DatabaseConstants.VIOLATIONS_VERBOSE_COLUMN) + " TEXT NOT NULL, " +
                            q(DatabaseConstants.VIOLATIONS_VL_COLUMN) + " INTEGER NOT NULL, " +
                            q(DatabaseConstants.VIOLATIONS_SENS_X_COLUMN) + " INTEGER NOT NULL, " +
                            q(DatabaseConstants.VIOLATIONS_SENS_Y_COLUMN) + " INTEGER NOT NULL, " +
                            q(DatabaseConstants.VIOLATIONS_PING_COLUMN) + " INTEGER NOT NULL, " +
                            q(DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN) + " BIGINT NOT NULL, " +
                            "FOREIGN KEY (" + q(DatabaseConstants.VIOLATIONS_SERVER_ID_COLUMN) + ") REFERENCES " + q(DatabaseConstants.SERVERS_TABLE) + "(" + q("id") + "), " +
                            "FOREIGN KEY (" + q(DatabaseConstants.VIOLATIONS_CHECK_NAME_ID_COLUMN) + ") REFERENCES " + q(DatabaseConstants.CHECK_NAMES_TABLE) + "(" + q("id") + "), " +
                            "FOREIGN KEY (" + q(DatabaseConstants.VIOLATIONS_CLIENT_BRAND_ID_COLUMN) + ") REFERENCES " + q(DatabaseConstants.CLIENT_BRANDS_TABLE) + "(" + q("id") + "), " +
                            "FOREIGN KEY (" + q(DatabaseConstants.VIOLATIONS_CLIENT_VERSION_ID_COLUMN) + ") REFERENCES " + q(DatabaseConstants.CLIENT_VERSIONS_TABLE) + "(" + q("id") + ")" +
                            ")")) {
                ps.execute();
            }

            createIndex(c, "idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_uuid", DatabaseConstants.VIOLATIONS_TABLE, DatabaseConstants.VIOLATIONS_UUID_COLUMN);
            createIndex(c, "idx_" + DatabaseConstants.VIOLATIONS_TABLE + "_created_at", DatabaseConstants.VIOLATIONS_TABLE, DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN);
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to initialize history database", ex);
            throw ex;
        }
    }

    private void createLookupTable(Connection c, String table, String column, String pk) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("CREATE TABLE IF NOT EXISTS " + q(table) + "(" + q("id") + " " + pk + ", " + q(column) + " VARCHAR(255) NOT NULL UNIQUE)")) {
            ps.execute();
        }
        createIndex(c, "idx_" + table + "_value", table, column);
    }

    private void createIndex(Connection c, String indexName, String table, String column) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("CREATE INDEX IF NOT EXISTS " + q(indexName) + " ON " + q(table) + "(" + q(column) + ")")) {
            ps.execute();
        }
    }

    @Override
    public synchronized void logAlert(ViolationRecord r) {
        try (Connection c = getConnection()) {
            long serverId = DatabaseUtils.getOrCreateId(c, dialect, DatabaseConstants.SERVERS_TABLE, DatabaseConstants.SERVERS_STRING_COLUMN, r.server());
            long checkId = DatabaseUtils.getOrCreateId(c, dialect, DatabaseConstants.CHECK_NAMES_TABLE, DatabaseConstants.CHECK_NAMES_STRING_COLUMN, r.checkName());
            long brandId = DatabaseUtils.getOrCreateId(c, dialect, DatabaseConstants.CLIENT_BRANDS_TABLE, DatabaseConstants.CLIENT_BRANDS_STRING_COLUMN, r.clientBrand());
            long clientVersionId = DatabaseUtils.getOrCreateId(c, dialect, DatabaseConstants.CLIENT_VERSIONS_TABLE, DatabaseConstants.CLIENT_VERSIONS_STRING_COLUMN, r.clientVersion());

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO " + q(DatabaseConstants.VIOLATIONS_TABLE) + " (" +
                            q(DatabaseConstants.VIOLATIONS_UUID_COLUMN) + ", " +
                            q(DatabaseConstants.VIOLATIONS_SERVER_ID_COLUMN) + ", " +
                            q(DatabaseConstants.VIOLATIONS_CHECK_NAME_ID_COLUMN) + ", " +
                            q(DatabaseConstants.VIOLATIONS_CLIENT_BRAND_ID_COLUMN) + ", " +
                            q(DatabaseConstants.VIOLATIONS_CLIENT_VERSION_ID_COLUMN) + ", " +
                            q(DatabaseConstants.VIOLATIONS_VERBOSE_COLUMN) + ", " +
                            q(DatabaseConstants.VIOLATIONS_VL_COLUMN) + ", " +
                            q(DatabaseConstants.VIOLATIONS_SENS_X_COLUMN) + ", " +
                            q(DatabaseConstants.VIOLATIONS_SENS_Y_COLUMN) + ", " +
                            q(DatabaseConstants.VIOLATIONS_PING_COLUMN) + ", " +
                            q(DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN) +
                            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setBytes(1, DatabaseUtils.uuidToBytes(r.uuid()));
                ps.setLong(2, serverId);
                ps.setLong(3, checkId);
                ps.setLong(4, brandId);
                ps.setLong(5, clientVersionId);
                ps.setString(6, r.verbose());
                ps.setInt(7, r.vl());
                ps.setInt(8, r.sensitivityX());
                ps.setInt(9, r.sensitivityY());
                ps.setInt(10, r.ping());
                ps.setLong(11, r.createdAt());
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            if (isConnectionError(ex)) connectionLost(ex);
            logger.log(Level.SEVERE, "Failed to insert history record", ex);
        }
    }

    @Override
    public synchronized int getLogCount(UUID uuid) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM " + q(DatabaseConstants.VIOLATIONS_TABLE) + " WHERE " + q(DatabaseConstants.VIOLATIONS_UUID_COLUMN) + " = ?")) {
            ps.setBytes(1, DatabaseUtils.uuidToBytes(uuid));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            if (isConnectionError(ex)) connectionLost(ex);
            logger.log(Level.SEVERE, "Failed to count history records", ex);
            return 0;
        }
    }

    @Override
    public synchronized List<ViolationRecord> getViolations(UUID uuid, int page, int limit) {
        List<ViolationRecord> out = new ArrayList<>();
        int offset = Math.max(0, (page - 1)) * limit;
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT v." + q(DatabaseConstants.VIOLATIONS_UUID_COLUMN) + ", v." + q(DatabaseConstants.VIOLATIONS_VERBOSE_COLUMN) + ", v." + q(DatabaseConstants.VIOLATIONS_VL_COLUMN) + ", v." + q(DatabaseConstants.VIOLATIONS_SENS_X_COLUMN) + ", v." + q(DatabaseConstants.VIOLATIONS_SENS_Y_COLUMN) + ", v." + q(DatabaseConstants.VIOLATIONS_PING_COLUMN) + ", v." + q(DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN) + ", " +
                             "s." + q(DatabaseConstants.SERVERS_STRING_COLUMN) + ", cn." + q(DatabaseConstants.CHECK_NAMES_STRING_COLUMN) + ", cb." + q(DatabaseConstants.CLIENT_BRANDS_STRING_COLUMN) + ", cv." + q(DatabaseConstants.CLIENT_VERSIONS_STRING_COLUMN) + " " +
                             "FROM " + q(DatabaseConstants.VIOLATIONS_TABLE) + " v " +
                             "JOIN " + q(DatabaseConstants.SERVERS_TABLE) + " s ON v." + q(DatabaseConstants.VIOLATIONS_SERVER_ID_COLUMN) + " = s." + q("id") + " " +
                             "JOIN " + q(DatabaseConstants.CHECK_NAMES_TABLE) + " cn ON v." + q(DatabaseConstants.VIOLATIONS_CHECK_NAME_ID_COLUMN) + " = cn." + q("id") + " " +
                             "JOIN " + q(DatabaseConstants.CLIENT_BRANDS_TABLE) + " cb ON v." + q(DatabaseConstants.VIOLATIONS_CLIENT_BRAND_ID_COLUMN) + " = cb." + q("id") + " " +
                             "JOIN " + q(DatabaseConstants.CLIENT_VERSIONS_TABLE) + " cv ON v." + q(DatabaseConstants.VIOLATIONS_CLIENT_VERSION_ID_COLUMN) + " = cv." + q("id") + " " +
                             "WHERE v." + q(DatabaseConstants.VIOLATIONS_UUID_COLUMN) + " = ? ORDER BY v." + q(DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN) + " DESC LIMIT ? OFFSET ?")) {
            ps.setBytes(1, DatabaseUtils.uuidToBytes(uuid));
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ViolationRecord(
                            rs.getString(DatabaseConstants.SERVERS_STRING_COLUMN),
                            DatabaseUtils.bytesToUuid(rs.getBytes(DatabaseConstants.VIOLATIONS_UUID_COLUMN)),
                            rs.getString(DatabaseConstants.CHECK_NAMES_STRING_COLUMN),
                            rs.getString(DatabaseConstants.VIOLATIONS_VERBOSE_COLUMN),
                            rs.getInt(DatabaseConstants.VIOLATIONS_VL_COLUMN),
                            rs.getLong(DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN),
                            rs.getString(DatabaseConstants.CLIENT_BRANDS_STRING_COLUMN),
                            rs.getString(DatabaseConstants.CLIENT_VERSIONS_STRING_COLUMN),
                            rs.getInt(DatabaseConstants.VIOLATIONS_SENS_X_COLUMN),
                            rs.getInt(DatabaseConstants.VIOLATIONS_SENS_Y_COLUMN),
                            rs.getInt(DatabaseConstants.VIOLATIONS_PING_COLUMN)
                    ));
                }
            }
        } catch (SQLException ex) {
            if (isConnectionError(ex)) connectionLost(ex);
            logger.log(Level.SEVERE, "Failed to fetch history records", ex);
        }
        return out;
    }

    @Override
    public synchronized int clear(UUID uuid) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM " + q(DatabaseConstants.VIOLATIONS_TABLE) + " WHERE " + q(DatabaseConstants.VIOLATIONS_UUID_COLUMN) + " = ?")) {
            ps.setBytes(1, DatabaseUtils.uuidToBytes(uuid));
            return ps.executeUpdate();
        } catch (SQLException ex) {
            if (isConnectionError(ex)) connectionLost(ex);
            logger.log(Level.SEVERE, "Failed to clear history", ex);
            return 0;
        }
    }

    @Override
    public synchronized int purgeOlderThan(long cutoffMillis) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM " + q(DatabaseConstants.VIOLATIONS_TABLE) + " WHERE " + q(DatabaseConstants.VIOLATIONS_CREATED_AT_COLUMN) + " < ?")) {
            ps.setLong(1, cutoffMillis);
            return ps.executeUpdate();
        } catch (SQLException ex) {
            if (isConnectionError(ex)) connectionLost(ex);
            logger.log(Level.SEVERE, "Failed to purge old history", ex);
            return 0;
        }
    }

    @Override
    public synchronized void disconnect() {
        closeQuietly();
    }

    private String q(String identifier) {
        return dialect.quoteIdentifier(identifier);
    }

    private static boolean isConnectionError(SQLException ex) {
        if (ex instanceof SQLNonTransientConnectionException || ex instanceof SQLTransientConnectionException) {
            return true;
        }
        String state = ex.getSQLState();
        return state != null && state.startsWith("08");
    }

    private void connectionLost(SQLException cause) {
        closeQuietly();
        throw new DatabaseUnavailableException("Connection to history database lost", cause);
    }

    private void closeQuietly() {
        try {
            if (openConnection != null && !openConnection.isClosed()) openConnection.close();
        } catch (SQLException ignored) {
        } finally {
            openConnection = null;
        }
    }

    private Connection getConnection() throws SQLException {
        if (openConnection == null || openConnection.isClosed()) {
            registerDriver();
            openConnection = DriverManager.getConnection(jdbcUrl, connectionProperties);
        }
        return openConnection;
    }

    private void registerDriver() throws SQLException {
        Driver driver = dialect.newDriver();
        if (REGISTERED_DRIVERS.add(driver.getClass().getName())) {
            DriverManager.registerDriver(driver);
        }
    }
}
