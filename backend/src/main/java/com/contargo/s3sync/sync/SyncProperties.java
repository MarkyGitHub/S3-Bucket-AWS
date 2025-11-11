package com.contargo.s3sync.sync;

/**
 * Configuration properties controlling the sync scheduler.
 */
import java.time.Duration;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "sync")
@Validated
public class SyncProperties {

    @NotNull(message = "sync.schedule-interval must not be null")
    @DurationMin(seconds = 1, message = "sync.schedule-interval must be at least 1 second")
    private Duration scheduleInterval = Duration.ofHours(3);

    private boolean schedulerEnabled = true;

    public Duration getScheduleInterval() {
        return scheduleInterval;
    }

    public void setScheduleInterval(Duration scheduleInterval) {
        this.scheduleInterval = scheduleInterval;
    }

    public boolean isSchedulerEnabled() {
        return schedulerEnabled;
    }

    public void setSchedulerEnabled(boolean schedulerEnabled) {
        this.schedulerEnabled = schedulerEnabled;
    }
}
