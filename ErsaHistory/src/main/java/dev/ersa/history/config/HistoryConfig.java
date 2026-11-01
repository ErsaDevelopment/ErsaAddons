package dev.ersa.history.config;

import dev.ersa.history.database.api.DatabaseType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HistoryConfig {
    private final boolean enabled;
    private final String commandMarker;
    private final int entriesPerPage;
    private final int retentionDays;
    private final int retentionIntervalMinutes;
    private final String prefix;
    private final String serverName;

    private final DatabaseType databaseType;
    private final String databaseFile;
    private final String databaseHost;
    private final int databasePort;
    private final String databaseName;
    private final String databaseUser;
    private final String databasePassword;
    private final Map<String, String> databaseProperties;
    private final int databaseTimeoutSeconds;
    private final int reconnectIntervalSeconds;

    private final String msgHeader, msgEntry, msgNoViolations, msgHistoryDisabled,
            msgLoadFailure, msgPlayerNotFound, msgInvalidPage, msgCleared, msgClearUsage, msgUsage;

    public HistoryConfig(FileConfiguration cfg) {
        this.enabled = cfg.getBoolean("enabled", true);
        this.commandMarker = cfg.getString("command-marker", "[history]");
        this.entriesPerPage = Math.max(1, cfg.getInt("entries-per-page", 15));
        this.retentionDays = Math.max(0, cfg.getInt("retention-days", 30));
        this.retentionIntervalMinutes = Math.max(1, cfg.getInt("retention-interval-minutes", 60));
        this.prefix = cfg.getString("prefix", "&8[&bErsaHistory&8]&r");
        this.serverName = cfg.getString("server-name", "default");

        this.databaseType = DatabaseType.fromString(cfg.getString("database-type", "sqlite"));
        this.databaseFile = cfg.getString("database-file", "history.sqlite");
        this.databaseHost = cfg.getString("database-host", "localhost");
        this.databasePort = cfg.getInt("database-port", 5432);
        this.databaseName = cfg.getString("database-name", "ersa_history");
        this.databaseUser = cfg.getString("database-user", "postgres");
        this.databasePassword = cfg.getString("database-password", "");
        this.databaseTimeoutSeconds = Math.max(1, cfg.getInt("database-timeout-seconds", 10));
        this.reconnectIntervalSeconds = Math.max(0, cfg.getInt("reconnect-interval-seconds", 60));

        Map<String, String> properties = new LinkedHashMap<>();
        ConfigurationSection propertiesSection = cfg.getConfigurationSection("database-properties");
        if (propertiesSection != null) {
            for (String key : propertiesSection.getKeys(false)) {
                Object value = propertiesSection.get(key);
                if (value != null) properties.put(key, String.valueOf(value));
            }
        }
        this.databaseProperties = Collections.unmodifiableMap(properties);

        this.msgHeader = cfg.getString("messages.header", "%prefix% &fИстория игрока &e%player% &7[&f%page% &7/ &f%maxPages%&7]");
        this.msgEntry = cfg.getString("messages.entry", "&c> &b%check% &c| &b%vl% VLs &c| &b%timeago% &c| &7%verbose%");
        this.msgNoViolations = cfg.getString("messages.no-violations", "%prefix% &7У игрока &f%player% &7нет записей.");
        this.msgHistoryDisabled = cfg.getString("messages.history-disabled", "%prefix% &cИстория отключена в конфиге.");
        this.msgLoadFailure = cfg.getString("messages.history-load-failure", "%prefix% &cИстория не загрузилась.");
        this.msgPlayerNotFound = cfg.getString("messages.player-not-found", "%prefix% &cИгрок не найден.");
        this.msgInvalidPage = cfg.getString("messages.invalid-page", "%prefix% &cНеверная страница.");
        this.msgCleared = cfg.getString("messages.cleared", "%prefix% &aИстория игрока &f%player% &aочищена (&f%count% &aзаписей).");
        this.msgClearUsage = cfg.getString("messages.clear-usage", "%prefix% &7Использование: &f/ersa history clear <player>");
        this.msgUsage = cfg.getString("messages.usage", "%prefix% &7Использование: &f/ersa history <player> [page]&7 или &f/ersa history clear <player>");
    }

    public boolean enabled() { return enabled; }
    public String commandMarker() { return commandMarker; }
    public int entriesPerPage() { return entriesPerPage; }
    public int retentionDays() { return retentionDays; }
    public int retentionIntervalMinutes() { return retentionIntervalMinutes; }
    public String prefix() { return prefix; }
    public String serverName() { return serverName; }
    public DatabaseType databaseType() { return databaseType; }
    public String databaseFile() { return databaseFile; }
    public String databaseHost() { return databaseHost; }
    public int databasePort() { return databasePort; }
    public String databaseName() { return databaseName; }
    public String databaseUser() { return databaseUser; }
    public String databasePassword() { return databasePassword; }
    public Map<String, String> databaseProperties() { return databaseProperties; }
    public int databaseTimeoutSeconds() { return databaseTimeoutSeconds; }
    public int reconnectIntervalSeconds() { return reconnectIntervalSeconds; }
    public String msgHeader() { return msgHeader; }
    public String msgEntry() { return msgEntry; }
    public String msgNoViolations() { return msgNoViolations; }
    public String msgHistoryDisabled() { return msgHistoryDisabled; }
    public String msgLoadFailure() { return msgLoadFailure; }
    public String msgPlayerNotFound() { return msgPlayerNotFound; }
    public String msgInvalidPage() { return msgInvalidPage; }
    public String msgCleared() { return msgCleared; }
    public String msgClearUsage() { return msgClearUsage; }
    public String msgUsage() { return msgUsage; }
}