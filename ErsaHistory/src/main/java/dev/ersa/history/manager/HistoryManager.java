package dev.ersa.history.manager;

import dev.ersa.history.config.HistoryConfig;
import dev.ersa.history.database.api.DatabaseDialect;
import dev.ersa.history.database.api.DatabaseType;
import dev.ersa.history.database.api.DatabaseUnavailableException;
import dev.ersa.history.database.api.HistoryStorage;
import dev.ersa.history.database.impl.JdbcHistoryStorage;
import dev.ersa.history.database.impl.NoOpHistoryStorage;
import dev.ersa.history.database.impl.postgres.PostgresDialect;
import dev.ersa.history.database.impl.sqlite.SQLiteDialect;
import dev.ersa.history.model.ViolationRecord;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;

public final class HistoryManager {
    private final Plugin plugin;
    private HistoryConfig config;
    private HistoryStorage storage = NoOpHistoryStorage.INSTANCE;
    private volatile boolean available = false;
    private BukkitTask reconnectTask;

    public HistoryManager(Plugin plugin, HistoryConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public boolean isLoaded() { return available; }

    public HistoryConfig getConfig() { return config; }

    public void init() {
        if (!config.enabled()) {
            plugin.getLogger().info("History is disabled in config.");
            return;
        }
        storage = createStorage();
        scheduleReconnectTask();

        if (plugin.getServer().isPrimaryThread()) {
            runAsync(this::ensureConnected);
        } else {
            ensureConnected();
        }
    }

    private synchronized void ensureConnected() {
        if (available) return;
        if (!config.enabled()) return;
        try {
            storage.connect();
            available = true;
            plugin.getLogger().info("History database connected (" + config.databaseType() + ").");
        } catch (SQLException ex) {
            available = false;
            int interval = config.reconnectIntervalSeconds();
            String suffix = interval > 0
                    ? ", retrying every " + interval + "s"
                    : " (automatic reconnect is disabled)";
            plugin.getLogger().log(Level.WARNING, "History database is unavailable" + suffix, ex);
        } catch (Throwable ex) {
            available = false;
            plugin.getLogger().log(Level.WARNING, "Unexpected error while connecting to history database", ex);
        }
    }

    private void scheduleReconnectTask() {
        cancelReconnectTask();
        int seconds = config.reconnectIntervalSeconds();
        if (seconds <= 0) return;
        long ticks = Math.max(20L, seconds * 20L);
        reconnectTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (!available) ensureConnected();
        }, ticks, ticks);
    }

    private void cancelReconnectTask() {
        if (reconnectTask != null) {
            try { reconnectTask.cancel(); } catch (Throwable ignored) {}
            reconnectTask = null;
        }
    }

    public void reload(HistoryConfig newConfig) {
        shutdown();
        this.config = newConfig;
        init();
    }

    public void shutdown() {
        cancelReconnectTask();
        try { storage.disconnect(); } catch (Throwable ignored) {
        } finally {
            storage = NoOpHistoryStorage.INSTANCE;
            available = false;
        }
    }

    public void logAlert(ViolationRecord record) {
        if (!available) return;
        final HistoryStorage target = this.storage;
        runAsync(() -> {
            try {
                target.logAlert(record);
            } catch (DatabaseUnavailableException ex) {
                handleConnectionLoss(ex);
            }
        });
    }

    public int getLogCount(UUID uuid) {
        if (!available) return 0;
        try {
            return storage.getLogCount(uuid);
        } catch (DatabaseUnavailableException ex) {
            handleConnectionLoss(ex);
            return 0;
        }
    }

    public List<ViolationRecord> getViolations(UUID uuid, int page, int limit) {
        if (!available) return List.of();
        try {
            return storage.getViolations(uuid, page, limit);
        } catch (DatabaseUnavailableException ex) {
            handleConnectionLoss(ex);
            return List.of();
        }
    }

    public int clear(UUID uuid) {
        if (!available) return 0;
        try {
            return storage.clear(uuid);
        } catch (DatabaseUnavailableException ex) {
            handleConnectionLoss(ex);
            return 0;
        }
    }

    public int purgeOlderThan(long cutoffMillis) {
        if (!available) return 0;
        try {
            return storage.purgeOlderThan(cutoffMillis);
        } catch (DatabaseUnavailableException ex) {
            handleConnectionLoss(ex);
            return 0;
        }
    }

    private void handleConnectionLoss(DatabaseUnavailableException ex) {
        boolean wasAvailable = available;
        available = false;
        if (wasAvailable) {
            plugin.getLogger().log(Level.WARNING,
                    "Lost connection to history database, it will be reconnected automatically", ex.getCause());
        }
        try { storage.disconnect(); } catch (Throwable ignored) {}
    }

    private void runAsync(Runnable task) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }

    private HistoryStorage createStorage() {
        final DatabaseDialect dialect;
        final String jdbcUrl;
        final Properties properties = new Properties();
        final int timeoutSeconds = Math.max(1, config.databaseTimeoutSeconds());

        if (config.databaseType() == DatabaseType.POSTGRESQL) {
            dialect = new PostgresDialect();
            jdbcUrl = "jdbc:postgresql://" + config.databaseHost() + ":" + config.databasePort() + "/" + config.databaseName();
            properties.setProperty("user", config.databaseUser());
            properties.setProperty("password", config.databasePassword());
            properties.setProperty("connectTimeout", String.valueOf(timeoutSeconds));
            properties.setProperty("socketTimeout", String.valueOf(timeoutSeconds));
        } else {
            dialect = new SQLiteDialect();
            jdbcUrl = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + File.separator + config.databaseFile();
            properties.setProperty("busy_timeout", String.valueOf(timeoutSeconds * 1000L));
        }

        config.databaseProperties().forEach(properties::setProperty);
        return new JdbcHistoryStorage(plugin, dialect, jdbcUrl, properties);
    }
}
