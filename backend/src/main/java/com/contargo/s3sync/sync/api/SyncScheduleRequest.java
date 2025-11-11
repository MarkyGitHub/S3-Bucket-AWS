package com.contargo.s3sync.sync.api;

/**
 * Request body to update the scheduler interval.
 */
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class SyncScheduleRequest {

    @Min(value = 0, message = "hours must not be negative")
    private long hours;

    @Min(value = 0, message = "minutes must not be negative")
    @Max(value = 59, message = "minutes must be less than 60")
    private long minutes;

    public long getHours() {
        return hours;
    }

    public void setHours(long hours) {
        this.hours = hours;
    }

    public long getMinutes() {
        return minutes;
    }

    public void setMinutes(long minutes) {
        this.minutes = minutes;
    }

    @AssertTrue(message = "interval must be greater than zero")
    public boolean isPositiveInterval() {
        return hours > 0 || minutes > 0;
    }
}

