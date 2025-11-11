package com.contargo.s3sync.config;

/**
 * Configuration properties for S3 connectivity and addressing.
 */
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "aws.s3")
@Validated
public class S3Properties {

    private static final String URI_REGEX = "^(http|https)://.+";

    @NotBlank(message = "aws.s3.bucket-name must be provided")
    private String bucketName;

    @NotBlank(message = "aws.s3.region must be provided")
    private String region;

    @Pattern(regexp = URI_REGEX, message = "aws.s3.endpoint must be a valid http(s) URL")
    private String endpoint;

    private boolean forcePathStyle;

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            this.endpoint = null;
        } else {
            this.endpoint = endpoint;
        }
    }

    public boolean isForcePathStyle() {
        return forcePathStyle;
    }

    public void setForcePathStyle(boolean forcePathStyle) {
        this.forcePathStyle = forcePathStyle;
    }
}
