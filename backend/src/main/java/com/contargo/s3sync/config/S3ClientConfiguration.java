package com.contargo.s3sync.config;

/**
 * Configures the AWS SDK S3 client used by the application.
 * Supports optional endpoint override and path-style access for local stacks.
 */
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class S3ClientConfiguration {

    private final S3Properties properties;

    public S3ClientConfiguration(S3Properties properties) {
        this.properties = properties;
    }

    @Bean
    /**
     * Builds and configures a reusable {@link S3Client}.
     */
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
            .region(Region.of(properties.getRegion()));

        if (properties.isForcePathStyle()) {
            builder = builder.serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build());
        }

        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder = builder.endpointOverride(java.net.URI.create(properties.getEndpoint()))
                .credentialsProvider(localStackCredentials());
        } else {
            builder = builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    /**
     * Simple static credentials for local test environments (e.g. LocalStack).
     */
    private AwsCredentialsProvider localStackCredentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));
    }
}

