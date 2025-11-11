package com.contargo.s3sync.sync.api;

/**
 * Details for a single exported batch within a run.
 */
public record SyncRunItemResponse(
    String tableName,
    String country,
    int objectCount,
    String s3Key
) {
}

