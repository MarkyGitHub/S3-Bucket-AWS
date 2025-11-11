package com.contargo.s3sync.sync.api;

/**
 * Summary of the last successful sync timestamp per table.
 */
import java.time.OffsetDateTime;

public record SyncStateResponse(
    String tableName,
    OffsetDateTime lastSuccessfulSync
) {
}

