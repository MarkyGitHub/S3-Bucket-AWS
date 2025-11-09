package com.contargo.s3sync.sync.api;

import java.time.OffsetDateTime;

public record SyncStateResponse(
    String tableName,
    OffsetDateTime lastSuccessfulSync
) {
}

