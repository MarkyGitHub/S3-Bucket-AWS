package com.contargo.s3sync.s3;

/** Minimal S3 object metadata used by the UI. */
import java.time.Instant;

public record S3ObjectMetadata(String key, long size, Instant lastModified) {

}
