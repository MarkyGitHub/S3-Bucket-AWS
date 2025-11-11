package com.contargo.s3sync.sync;

/**
 * Manages periodic execution of the synchronization process.
 * Allows enabling/disabling and live updates to the schedule interval.
 */
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class SyncScheduler implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private final TaskScheduler taskScheduler;
    private final SyncService syncService;
    private final SyncProperties syncProperties;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final Object monitor = new Object();
    private ScheduledFuture<?> scheduledTask;
    private Duration currentInterval;

    public SyncScheduler(TaskScheduler taskScheduler, SyncService syncService, SyncProperties syncProperties) {
        this.taskScheduler = taskScheduler;
        this.syncService = syncService;
        this.syncProperties = syncProperties;
    }

    @PostConstruct
    /**
     * Initializes the scheduler and schedules the first task if enabled via configuration.
     */
    public void initialize() {
        Duration configuredInterval = syncProperties.getScheduleInterval();
        Objects.requireNonNull(configuredInterval, "scheduleInterval must not be null");

        synchronized (monitor) {
            currentInterval = configuredInterval;
            if (syncProperties.isSchedulerEnabled()) {
                schedule(configuredInterval);
            } else {
                log.info("Sync scheduler disabled via configuration; manual runs only");
            }
        }
    }

    /**
     * Returns the current interval used by the scheduler.
     */
    public Duration getCurrentInterval() {
        synchronized (monitor) {
            return currentInterval;
        }
    }

    /**
     * Indicates whether the scheduler is currently enabled.
     */
    public boolean isSchedulerEnabled() {
        return syncProperties.isSchedulerEnabled();
    }

    /**
     * Updates the schedule interval and (re)schedules the task.
     *
     * @param interval the new interval; must be > 0
     */
    public void updateInterval(Duration interval) {
        if (interval == null || interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("Interval must be greater than zero");
        }

        synchronized (monitor) {
            log.info("Updating sync schedule to every {}", humanReadable(interval));
            syncProperties.setScheduleInterval(interval);
            if (!syncProperties.isSchedulerEnabled()) {
                syncProperties.setSchedulerEnabled(true);
            }
            currentInterval = interval;
            schedule(interval);
        }
    }

    /**
     * Disables scheduling and cancels the current scheduled task.
     */
    public void disable() {
        synchronized (monitor) {
            log.info("Disabling scheduled sync");
            syncProperties.setSchedulerEnabled(false);
            cancelScheduledTask();
        }
    }

    private void schedule(Duration interval) {
        cancelScheduledTask();
        scheduledTask = taskScheduler.scheduleAtFixedRate(this::runSafely, Objects.requireNonNull(interval));
        log.info("Scheduled sync task every {}", humanReadable(interval));
    }

    /**
     * Guards against overlapping runs and logs failures without crashing the scheduler thread.
     */
    private void runSafely() {
        if (!syncProperties.isSchedulerEnabled()) {
            return;
        }

        if (!running.compareAndSet(false, true)) {
            log.warn("Previous sync still running; skipping this scheduled execution");
            return;
        }

        try {
            log.info("Starting scheduled sync");
            SyncRun run = syncService.runSync();
            logSyncSummary(run);
        } catch (Exception ex) {
            log.error("Scheduled sync failed", ex);
        } finally {
            running.set(false);
        }
    }

    private void cancelScheduledTask() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
    }

    /**
     * Produces a human-friendly textual representation of an interval.
     */
    private String humanReadable(Duration interval) {
        long hours = interval.toHours();
        long minutes = interval.minusHours(hours).toMinutes();
        if (hours > 0 && minutes > 0) {
            return String.format("%d hour(s) and %d minute(s)", hours, minutes);
        } else if (hours > 0) {
            return String.format("%d hour(s)", hours);
        } else {
            return String.format("%d minute(s)", minutes);
        }
    }

    @Override
    /**
     * Cancels scheduled work on bean destruction.
     */
    public void destroy() {
        synchronized (monitor) {
            cancelScheduledTask();
        }
    }

    /**
     * Logs a concise summary of the items produced by a scheduled run.
     */
    private void logSyncSummary(SyncRun run) {
        if (run == null || run.getItems().isEmpty()) {
            log.info("Scheduled sync finished without data changes");
            return;
        }

        int batchCount = run.getItems().size();
        int totalRows = run.getItems().stream()
                .mapToInt(SyncRunItem::getObjectCount)
                .sum();

        String details = run.getItems().stream()
                .map(item -> String.format("%s/%s: %d row(s) -> %s",
                item.getTableName(),
                item.getCountry(),
                item.getObjectCount(),
                item.getS3Key()))
                .collect(Collectors.joining("; "));

        log.info("Scheduled sync finished: {} batch(es), {} total row(s). Details: {}", batchCount, totalRows, details);
    }
}
