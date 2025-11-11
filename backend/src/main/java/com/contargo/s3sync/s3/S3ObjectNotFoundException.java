package com.contargo.s3sync.s3;

/**
 * Signals that a requested S3 object could not be found.
 */
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class S3ObjectNotFoundException extends RuntimeException {

    public S3ObjectNotFoundException(String key, Throwable cause) {
        super("S3 object not found: " + key, cause);
    }
}


