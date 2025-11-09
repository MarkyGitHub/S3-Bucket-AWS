package com.contargo.s3sync.sync.api;

public record SyncRunItemResponse(
    String tableName,
    String country,
    int objectCount,
    String s3Key
) {
}

