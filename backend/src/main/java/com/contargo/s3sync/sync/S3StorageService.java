package com.contargo.s3sync.sync;

/**
 * Low-level utility for writing CSV exports to S3.
 * Ensures the bucket exists, builds stable keys, and retries transient upload failures.
 */
import com.contargo.s3sync.config.S3Properties;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);
    private static final DateTimeFormatter DATE_FOLDER_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter FILE_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final int MAX_UPLOAD_ATTEMPTS = 3;

    private final S3Client s3Client;
    private final S3Properties properties;

    public S3StorageService(S3Client s3Client, S3Properties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    /**
     * Stores the given content as a CSV object and returns the S3 key used.
     *
     * @param tableName logical source table (e.g. "kunde", "auftraege")
     * @param country partition key used in the path (defaults to "unknown")
     * @param generationTime timestamp used for folder and filename
     * @param content CSV payload to upload
     * @return the S3 object key
     */
    public String store(String tableName, String country, OffsetDateTime generationTime, String content) {
        ensureBucketExists();

        String key = buildKey(tableName, country, generationTime);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(key)
                .contentType("text/csv")
                .build();

        for (int attempt = 1; attempt <= MAX_UPLOAD_ATTEMPTS; attempt++) {
            try {
                s3Client.putObject(request, RequestBody.fromString(content, StandardCharsets.UTF_8));
                log.info("Uploaded {} records for {}:{} to s3://{}/{} (attempt {}/{})", countLines(content), tableName,
                        country, properties.getBucketName(), key, attempt, MAX_UPLOAD_ATTEMPTS);
                return key;
            } catch (RuntimeException ex) {
                log.error("Failed to upload data for {}:{} to s3://{}/{} (attempt {}/{})", tableName, country,
                        properties.getBucketName(), key, attempt, MAX_UPLOAD_ATTEMPTS, ex);
                if (attempt == MAX_UPLOAD_ATTEMPTS) {
                    throw ex;
                }
                try {
                    TimeUnit.SECONDS.sleep(attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Upload interrupted while retrying S3 putObject", interrupted);
                }
            }
        }
        // Unreachable but required by compiler.
        return key;
    }

    /**
     * Creates the bucket if it does not yet exist.
     */
    private void ensureBucketExists() {
        try {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(properties.getBucketName())
                    .build());
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException ignored) {
            // ignore
        }
    }

    /**
     * Builds a deterministic S3 key from table name, date and country.
     */
    private String buildKey(String tableName, String country, OffsetDateTime generationTime) {
        String normalizedCountry = (country == null || country.isBlank()) ? "unknown" : country;
        String dateFolder = DATE_FOLDER_FORMAT.format(generationTime);

        if ("kunde".equals(tableName)) {
            return "%s/%s/%s/customers_%s_%s.csv".formatted(
                    tableName,
                    dateFolder,
                    normalizedCountry,
                    normalizedCountry,
                    dateFolder
            );
        }

        if ("auftraege".equals(tableName)) {
            return "%s/%s/%s/orders_%s_%s.csv".formatted(
                    tableName,
                    dateFolder,
                    normalizedCountry,
                    normalizedCountry,
                    dateFolder
            );
        }

        String timestamp = FILE_DATE_TIME_FORMAT.format(generationTime);
        return "%s/%s/%s/%s_%s.csv".formatted(tableName, dateFolder, normalizedCountry, tableName, timestamp);
    }

    /**
     * Counts non-empty lines to aid logging.
     */
    private long countLines(String content) {
        return content.lines().filter(line -> !line.isBlank()).count();
    }
}
