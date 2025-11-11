package com.contargo.s3sync.s3;

/** Wraps low-level S3 SDK errors into a domain-specific runtime exception. */
public class S3OperationException extends RuntimeException {

    public S3OperationException(String message, Throwable cause) {
        super(message, cause);
    }
}


