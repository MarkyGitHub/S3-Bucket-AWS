package com.contargo.s3sync.sync;

import com.contargo.s3sync.config.S3Properties;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
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
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final S3Client s3Client;
    private final S3Properties properties;

    public S3StorageService(S3Client s3Client, S3Properties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    public String store(String tableName, String country, OffsetDateTime generationTime, String content) {
        ensureBucketExists();

        String key = buildKey(tableName, country, generationTime);
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(properties.getBucketName())
            .key(key)
            .contentType("text/csv")
            .build();

        s3Client.putObject(request, RequestBody.fromString(content, StandardCharsets.UTF_8));
        log.info("Uploaded {} records for {}:{} to s3://{}/{}", countLines(content), tableName, country,
            properties.getBucketName(), key);
        return key;
    }

    private void ensureBucketExists() {
        try {
            s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(properties.getBucketName())
                .build());
        } catch (BucketAlreadyExistsException e) {
            // bucket already available
        } catch (BucketAlreadyOwnedByYouException e) {
            // ignore
        }
    }

    private String buildKey(String tableName, String country, OffsetDateTime generationTime) {
        return String.format("%s/%s_%s_%s.csv", tableName, tableName, country,
            FILE_DATE_FORMAT.format(generationTime));
    }

    private long countLines(String content) {
        return content.lines().filter(line -> !line.isBlank()).count();
    }
}

