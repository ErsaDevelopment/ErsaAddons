package dev.ersa.history.model;

import java.util.UUID;

public record ViolationRecord(
        String server,
        UUID uuid,
        String checkName,
        String verbose,
        int vl,
        long createdAt,
        String clientBrand,
        String clientVersion,
        int sensitivityX,
        int sensitivityY,
        int ping
) {}