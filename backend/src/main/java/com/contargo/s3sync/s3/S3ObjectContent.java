package com.contargo.s3sync.s3;

/** Binary object data and its content type. */
public record S3ObjectContent(byte[] data, String contentType) {
}


