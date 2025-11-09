package com.contargo.s3sync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.s3")
public class S3Properties {

    private String bucketName;
    private String region;
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
        this.endpoint = endpoint;
    }

    public boolean isForcePathStyle() {
        return forcePathStyle;
    }

    public void setForcePathStyle(boolean forcePathStyle) {
        this.forcePathStyle = forcePathStyle;
    }
}

