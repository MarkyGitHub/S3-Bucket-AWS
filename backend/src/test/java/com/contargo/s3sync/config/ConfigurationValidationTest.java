package com.contargo.s3sync.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.contargo.s3sync.sync.SyncProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class ConfigurationValidationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void failsWhenBucketNameMissing() {
        contextRunner
                .withPropertyValues(
                        "aws.s3.region=eu-central-1",
                        "sync.schedule-interval=3h"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void failsWhenRegionMissing() {
        contextRunner
                .withPropertyValues(
                        "aws.s3.bucket-name=test-bucket",
                        "sync.schedule-interval=3h"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void failsWhenScheduleIntervalNonPositive() {
        contextRunner
                .withPropertyValues(
                        "aws.s3.bucket-name=test-bucket",
                        "aws.s3.region=eu-central-1",
                        "sync.schedule-interval=0s"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void succeedsWithValidConfiguration() {
        contextRunner
                .withPropertyValues(
                        "aws.s3.bucket-name=test-bucket",
                        "aws.s3.region=eu-central-1",
                        "aws.s3.endpoint=http://localhost:4566",
                        "sync.schedule-interval=15m"
                )
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Configuration
    @EnableConfigurationProperties({S3Properties.class, SyncProperties.class})
    static class TestConfiguration {
        // registers configuration properties for validation tests
    }
}
