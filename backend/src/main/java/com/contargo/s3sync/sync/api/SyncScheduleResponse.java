package com.contargo.s3sync.sync.api;

/**
 * Response describing the effective scheduler interval and status.
 */
import java.time.Duration;

public class SyncScheduleResponse {

    private final long hours;
    private final long minutes;
    private final long intervalSeconds;
    private final String isoDuration;
    private final boolean schedulerEnabled;

    public SyncScheduleResponse(long hours, long minutes, long intervalSeconds, String isoDuration,
        boolean schedulerEnabled) {
        this.hours = hours;
        this.minutes = minutes;
        this.intervalSeconds = intervalSeconds;
        this.isoDuration = isoDuration;
        this.schedulerEnabled = schedulerEnabled;
    }

    public static SyncScheduleResponse from(Duration interval, boolean enabled) {
        long hours = interval.toHours();
        long minutes = interval.minusHours(hours).toMinutes();
        long seconds = interval.getSeconds();
        return new SyncScheduleResponse(hours, minutes, seconds, interval.toString(), enabled);
    }

    public long getHours() {
        return hours;
    }

    public long getMinutes() {
        return minutes;
    }

    public long getIntervalSeconds() {
        return intervalSeconds;
    }

    public String getIsoDuration() {
        return isoDuration;
    }

    public boolean isSchedulerEnabled() {
        return schedulerEnabled;
    }
}

