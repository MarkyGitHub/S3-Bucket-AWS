package com.contargo.s3sync.sync;

/**
 * Endpoints for reading and updating the sync schedule configuration.
 */
import com.contargo.s3sync.sync.api.SyncScheduleRequest;
import com.contargo.s3sync.sync.api.SyncScheduleResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/sync/schedule")
public class SyncScheduleController {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduleController.class);

    private final SyncScheduler syncScheduler;

    public SyncScheduleController(SyncScheduler syncScheduler) {
        this.syncScheduler = syncScheduler;
    }

    @GetMapping
    /**
     * Returns the current scheduler interval and enabled flag.
     */
    public SyncScheduleResponse getSchedule() {
        return SyncScheduleResponse.from(syncScheduler.getCurrentInterval(), syncScheduler.isSchedulerEnabled());
    }

    @PutMapping
    /**
     * Updates the scheduler interval according to the provided request.
     */
    public SyncScheduleResponse updateSchedule(@Valid @RequestBody SyncScheduleRequest request) {
        Duration interval = Duration.ofHours(request.getHours()).plusMinutes(request.getMinutes());
        log.info("Received request to update sync interval to {}", interval);
        try {
            syncScheduler.updateInterval(interval);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }

        return SyncScheduleResponse.from(syncScheduler.getCurrentInterval(), syncScheduler.isSchedulerEnabled());
    }
}

