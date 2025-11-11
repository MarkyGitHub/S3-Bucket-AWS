package com.contargo.s3sync.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

class S3ClientConfigurationTest {

    @Test
    void s3Client_usesStaticCredentialsWhenEndpointConfigured() {
        S3Properties properties = new S3Properties();
        properties.setRegion("eu-central-1");
        properties.setBucketName("test-bucket");
        properties.setEndpoint("http://localhost:4566");
        properties.setForcePathStyle(true);

        S3ClientBuilder builder = mock(S3ClientBuilder.class, RETURNS_SELF);
        S3Client client = mock(S3Client.class);

        when(builder.build()).thenReturn(client);

        S3ClientConfiguration configuration = new TestableS3ClientConfiguration(properties, builder);

        configuration.s3Client();

        verify(builder).region(Region.of("eu-central-1"));
        ArgumentCaptor<S3Configuration> configCaptor = ArgumentCaptor.forClass(S3Configuration.class);
        verify(builder).serviceConfiguration(configCaptor.capture());
        S3Configuration s3Configuration = configCaptor.getValue();
        assertThat(s3Configuration).isNotNull();
        assertThat(s3Configuration.pathStyleAccessEnabled()).isTrue();
        verify(builder).endpointOverride(URI.create("http://localhost:4566"));

        ArgumentCaptor<AwsCredentialsProvider> captor = ArgumentCaptor.forClass(AwsCredentialsProvider.class);
        verify(builder).credentialsProvider(captor.capture());

        AwsCredentialsProvider provider = captor.getValue();
        assertThat(provider).isInstanceOf(StaticCredentialsProvider.class);
        AwsCredentials credentials = provider.resolveCredentials();
        assertThat(credentials).isInstanceOf(AwsBasicCredentials.class);
        AwsBasicCredentials basicCredentials = (AwsBasicCredentials) credentials;
        assertThat(basicCredentials.accessKeyId()).isEqualTo("test");
        assertThat(basicCredentials.secretAccessKey()).isEqualTo("test");
    }

    @Test
    void s3Client_usesDefaultCredentialsWhenNoEndpointConfigured() {
        S3Properties properties = new S3Properties();
        properties.setRegion("eu-central-1");
        properties.setBucketName("test-bucket");

        S3ClientBuilder builder = mock(S3ClientBuilder.class, RETURNS_SELF);
        S3Client client = mock(S3Client.class);

        when(builder.build()).thenReturn(client);

        S3ClientConfiguration configuration = new TestableS3ClientConfiguration(properties, builder);

        configuration.s3Client();

        verify(builder).region(Region.of("eu-central-1"));
        ArgumentCaptor<AwsCredentialsProvider> captor = ArgumentCaptor.forClass(AwsCredentialsProvider.class);
        verify(builder).credentialsProvider(captor.capture());

        AwsCredentialsProvider provider = captor.getValue();
        assertThat(provider).isInstanceOf(DefaultCredentialsProvider.class);
    }

    private static class TestableS3ClientConfiguration extends S3ClientConfiguration {

        private final S3ClientBuilder builder;

        private TestableS3ClientConfiguration(S3Properties properties, S3ClientBuilder builder) {
            super(properties);
            this.builder = builder;
        }

        @Override
        S3ClientBuilder createBuilder() {
            return builder;
        }
    }
}
