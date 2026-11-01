package dev.ersa.history.database.impl;

import dev.ersa.history.database.api.HistoryStorage;
import dev.ersa.history.model.ViolationRecord;

import java.util.List;
import java.util.UUID;

public final class NoOpHistoryStorage implements HistoryStorage {
    public static final NoOpHistoryStorage INSTANCE = new NoOpHistoryStorage();
    private NoOpHistoryStorage() {}

    @Override public void connect() {}
    @Override public void disconnect() {}
    @Override public void logAlert(ViolationRecord r) {}
    @Override public int getLogCount(UUID uuid) { return 0; }
    @Override public List<ViolationRecord> getViolations(UUID uuid, int page, int limit) { return List.of(); }
    @Override public int clear(UUID uuid) { return 0; }
    @Override public int purgeOlderThan(long c) { return 0; }
}