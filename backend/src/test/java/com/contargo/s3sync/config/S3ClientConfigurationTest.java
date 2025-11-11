package com.contargo.s3sync.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
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

        S3ClientConfiguration configuration = new S3ClientConfiguration(properties);

        S3ClientBuilder builder = mock(S3ClientBuilder.class, RETURNS_SELF);
        S3Client client = mock(S3Client.class);

        try (MockedStatic<S3Client> s3ClientStatic = mockStatic(S3Client.class)) {
            s3ClientStatic.when(S3Client::builder).thenReturn(builder);
            when(builder.build()).thenReturn(client);

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
    }

    @Test
    void s3Client_usesDefaultCredentialsWhenNoEndpointConfigured() {
        S3Properties properties = new S3Properties();
        properties.setRegion("eu-central-1");
        properties.setBucketName("test-bucket");

        S3ClientConfiguration configuration = new S3ClientConfiguration(properties);

        S3ClientBuilder builder = mock(S3ClientBuilder.class, RETURNS_SELF);
        S3Client client = mock(S3Client.class);

        try (MockedStatic<S3Client> s3ClientStatic = mockStatic(S3Client.class)) {
            s3ClientStatic.when(S3Client::builder).thenReturn(builder);
            when(builder.build()).thenReturn(client);

            configuration.s3Client();

            verify(builder).region(Region.of("eu-central-1"));
            ArgumentCaptor<AwsCredentialsProvider> captor = ArgumentCaptor.forClass(AwsCredentialsProvider.class);
            verify(builder).credentialsProvider(captor.capture());

            AwsCredentialsProvider provider = captor.getValue();
            assertThat(provider).isInstanceOf(DefaultCredentialsProvider.class);
        }
    }
}

