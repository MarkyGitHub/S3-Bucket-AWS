package com.contargo.s3sync.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.contargo.s3sync.config.S3Properties;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    private S3StorageService s3StorageService;

    @BeforeEach
    void setUp() {
        S3Properties properties = new S3Properties();
        properties.setBucketName("test-bucket");
        properties.setRegion("eu-central-1");
        s3StorageService = new S3StorageService(s3Client, properties);
    }

    @Test
    void store_buildsCustomerCsvKeyWithCountryAndDate() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2025-01-15T10:15:30Z");

        String key = s3StorageService.store("kunde", "DE", timestamp, "content");

        ArgumentCaptor<CreateBucketRequest> bucketCaptor = ArgumentCaptor.forClass(CreateBucketRequest.class);
        verify(s3Client).createBucket(bucketCaptor.capture());
        assertThat(bucketCaptor.getValue().bucket()).isEqualTo("test-bucket");

        ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(putCaptor.capture(), any(RequestBody.class));

        assertThat(key).isEqualTo("kunde/2025-01-15/DE/customers_DE_2025-01-15.csv");
        assertThat(putCaptor.getValue().key()).isEqualTo("kunde/2025-01-15/DE/customers_DE_2025-01-15.csv");
    }

    @Test
    void store_buildsOrderCsvKeyWithCountryAndDate() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2024-12-24T08:00:00Z");

        String key = s3StorageService.store("auftraege", "FR", timestamp, "orders");

        ArgumentCaptor<CreateBucketRequest> bucketCaptor = ArgumentCaptor.forClass(CreateBucketRequest.class);
        verify(s3Client).createBucket(bucketCaptor.capture());
        assertThat(bucketCaptor.getValue().bucket()).isEqualTo("test-bucket");

        ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(putCaptor.capture(), any(RequestBody.class));

        assertThat(key).isEqualTo("auftraege/2024-12-24/FR/orders_FR_2024-12-24.csv");
        assertThat(putCaptor.getValue().key()).isEqualTo("auftraege/2024-12-24/FR/orders_FR_2024-12-24.csv");
    }

    @Test
    void store_retriesFailedUploadsAndLogsEachAttempt() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2025-04-01T12:00:00Z");

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("first failure"))
                .thenThrow(new RuntimeException("second failure"))
                .thenReturn(PutObjectResponse.builder().eTag("ok").build());

        Logger logger = (Logger) LoggerFactory.getLogger(S3StorageService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            String key = s3StorageService.store("kunde", "DE", timestamp, "content\nrow2");
            assertThat(key).isEqualTo("kunde/2025-04-01/DE/customers_DE_2025-04-01.csv");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        verify(s3Client).createBucket(any(CreateBucketRequest.class));
        verify(s3Client, times(3)).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        List<ILoggingEvent> events = appender.list;
        assertThat(events).hasSize(3);
        assertThat(events.get(0).getLevel()).isEqualTo(Level.ERROR);
        assertThat(events.get(0).getFormattedMessage()).contains("attempt 1/3");
        assertThat(events.get(1).getLevel()).isEqualTo(Level.ERROR);
        assertThat(events.get(1).getFormattedMessage()).contains("attempt 2/3");
        assertThat(events.get(2).getLevel()).isEqualTo(Level.INFO);
        assertThat(events.get(2).getFormattedMessage()).contains("attempt 3/3");
    }

    @Test
    void store_throwsAfterMaxRetriesAndLogsFailures() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2025-04-01T12:00:00Z");

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("first failure"))
                .thenThrow(new RuntimeException("second failure"))
                .thenThrow(new RuntimeException("third failure"));

        Logger logger = (Logger) LoggerFactory.getLogger(S3StorageService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            assertThatThrownBy(() -> s3StorageService.store("kunde", "DE", timestamp, "content"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("third failure");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        verify(s3Client).createBucket(any(CreateBucketRequest.class));
        verify(s3Client, times(3)).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        List<ILoggingEvent> events = appender.list;
        assertThat(events).hasSize(3);
        assertThat(events.get(0).getLevel()).isEqualTo(Level.ERROR);
        assertThat(events.get(0).getFormattedMessage()).contains("attempt 1/3");
        assertThat(events.get(1).getLevel()).isEqualTo(Level.ERROR);
        assertThat(events.get(1).getFormattedMessage()).contains("attempt 2/3");
        assertThat(events.get(2).getLevel()).isEqualTo(Level.ERROR);
        assertThat(events.get(2).getFormattedMessage()).contains("attempt 3/3");
    }
}
