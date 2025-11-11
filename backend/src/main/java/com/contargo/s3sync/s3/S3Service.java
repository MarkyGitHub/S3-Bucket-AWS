package com.contargo.s3sync.s3;

/**
 * Thin wrapper around the AWS SDK S3 client for listing and reading objects.
 * Translates SDK exceptions to domain-specific ones.
 */
import com.contargo.s3sync.config.S3Properties;
import java.util.List;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
public class S3Service {

    private final S3Client s3Client;
    private final S3Properties properties;

    public S3Service(S3Client s3Client, S3Properties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    /**
     * Lists all objects' metadata in the configured bucket.
     *
     * @return a list of object metadata; empty list if bucket does not exist
     */
    public List<S3ObjectMetadata> listAllObjects() {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
            .bucket(properties.getBucketName())
            .build();

        try {
            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            return response.contents().stream()
                .map(this::toMetadata)
                .toList();
        } catch (NoSuchBucketException e) {
            return List.of();
        } catch (S3Exception e) {
            throw new S3OperationException("Failed to list objects for bucket %s".formatted(properties.getBucketName()),
                e);
        }
    }

    /**
     * Retrieves and returns object content and content type for the given key.
     *
     * @param key object key in the bucket
     * @return the object content wrapper
     */
    public S3ObjectContent getObject(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(properties.getBucketName())
            .key(key)
            .build();

        try {
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(request);
            return new S3ObjectContent(objectBytes.asByteArray(), objectBytes.response().contentType());
        } catch (NoSuchKeyException e) {
            throw new S3ObjectNotFoundException(key, e);
        } catch (NoSuchBucketException e) {
            throw new S3ObjectNotFoundException(key, e);
        } catch (S3Exception e) {
            throw new S3OperationException("Failed to read object %s".formatted(key), e);
        }
    }

    /**
     * Maps an SDK S3Object to the simplified metadata type.
     */
    private S3ObjectMetadata toMetadata(S3Object object) {
        return new S3ObjectMetadata(object.key(), object.size(), object.lastModified());
    }

    /**
     * Returns true if the bucket has no objects or does not exist.
     */
    public boolean isBucketEmpty() {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
            .bucket(properties.getBucketName())
            .maxKeys(1)
            .build();

        try {
            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            return response.contents().isEmpty();
        } catch (NoSuchBucketException e) {
            return true;
        } catch (S3Exception e) {
            throw new S3OperationException("Failed to inspect bucket %s".formatted(properties.getBucketName()), e);
        }
    }
}


