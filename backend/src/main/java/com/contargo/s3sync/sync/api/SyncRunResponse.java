package com.contargo.s3sync.sync.api;

import java.time.OffsetDateTime;
import java.util.List;

public record SyncRunResponse(
    Long id,
    OffsetDateTime startedAt,
    OffsetDateTime finishedAt,
    String status,
    String errorMessage,
    List<SyncRunItemResponse> items
) {
}

