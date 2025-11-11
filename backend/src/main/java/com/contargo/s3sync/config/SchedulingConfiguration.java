package com.contargo.s3sync.config;

/**
 * Enables Spring scheduling and provides the task scheduler used by sync jobs.
 */
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.TaskScheduler;

@Configuration
@EnableScheduling
public class SchedulingConfiguration {

    @Bean
    /**
     * Dedicated single-threaded scheduler for running sync tasks sequentially.
     */
    public TaskScheduler syncTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("sync-scheduler-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.initialize();
        return scheduler;
    }
}

