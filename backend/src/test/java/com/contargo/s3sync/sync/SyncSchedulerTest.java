package com.contargo.s3sync.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"null", "unused"})
class SyncSchedulerTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private SyncService syncService;

    private ScheduledFuture<?> scheduledFuture;

    private SyncProperties syncProperties;
    private SyncScheduler syncScheduler;

    @BeforeEach
    void setUp() {
        syncProperties = new SyncProperties();
        syncProperties.setScheduleInterval(Duration.ofHours(3));
        syncProperties.setSchedulerEnabled(true);
        scheduledFuture = mock(ScheduledFuture.class);
        when(taskScheduler.scheduleAtFixedRate(
                ArgumentMatchers.notNull(Runnable.class),
                ArgumentMatchers.notNull(Duration.class)))
                .thenAnswer(invocation -> scheduledFuture);
        syncScheduler = new SyncScheduler(taskScheduler, syncService, syncProperties);
    }

    @Test
    void initializeSchedulesSyncEveryThreeHours() {
        syncScheduler.initialize();

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Duration> intervalCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(taskScheduler).scheduleAtFixedRate(taskCaptor.capture(), intervalCaptor.capture());
        Duration scheduledInterval = Objects.requireNonNull(intervalCaptor.getValue());
        assertThat(scheduledInterval).isEqualTo(Duration.ofHours(3));
    }

    @Test
    void scheduledTaskRunsAgainAfterCompletion() {
        when(syncService.runSync()).thenReturn(new SyncRun());
        syncScheduler.initialize();

        Runnable task = captureScheduledTask();

        task.run();
        task.run();

        verify(syncService, times(2)).runSync();
    }

    @Test
    void scheduledTaskContinuesAfterFailure() {
        when(syncService.runSync())
                .thenThrow(new RuntimeException("boom"))
                .thenReturn(new SyncRun());
        syncScheduler.initialize();

        Runnable task = captureScheduledTask();

        assertThatCode(task::run).doesNotThrowAnyException();
        task.run();

        verify(syncService, times(2)).runSync();
    }

    private Runnable captureScheduledTask() {
        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).scheduleAtFixedRate(taskCaptor.capture(), ArgumentMatchers.notNull(Duration.class));
        return Objects.requireNonNull(taskCaptor.getValue());
    }
}
