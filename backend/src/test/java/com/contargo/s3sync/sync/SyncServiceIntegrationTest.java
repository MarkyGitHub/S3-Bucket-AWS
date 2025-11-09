package com.contargo.s3sync.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.contargo.s3sync.config.S3Properties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.LocalStackContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
@ActiveProfiles("test")
class SyncServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("s3sync")
        .withUsername("s3sync")
        .withPassword("s3sync");

    @Container
    static LocalStackContainer localstack = new LocalStackContainer("localstack/localstack:3.4")
        .withServices(LocalStackContainer.Service.S3);

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("aws.s3.endpoint", () -> localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        registry.add("aws.s3.region", localstack::getRegion);
        registry.add("aws.s3.bucket-name", () -> "test-bucket");
    }

    @Autowired
    private SyncService syncService;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Properties s3Properties;

    @Test
    void runSync_exportsFilesToS3() {
        SyncRun run = syncService.runSync();

        assertThat(run.getStatus()).isEqualTo(SyncStatus.SUCCESS);

        var response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
            .bucket(s3Properties.getBucketName())
            .build());

        assertThat(response.contents()).isNotEmpty();
    }
}

