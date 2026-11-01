package dev.ersa.history.database.api;

import dev.ersa.history.model.ViolationRecord;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface HistoryStorage {
    void connect() throws SQLException;
    void disconnect();
    void logAlert(ViolationRecord record);
    int getLogCount(UUID uuid);
    List<ViolationRecord> getViolations(UUID uuid, int page, int limit);
    int clear(UUID uuid);
    int purgeOlderThan(long cutoffMillis);
}