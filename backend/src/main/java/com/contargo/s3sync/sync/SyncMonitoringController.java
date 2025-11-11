package com.contargo.s3sync.sync;

/**
 * Read-only endpoints to inspect recent sync runs and current sync state.
 */
import com.contargo.s3sync.sync.api.SyncRunResponse;
import com.contargo.s3sync.sync.api.SyncStateResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
public class SyncMonitoringController {

    private static final Logger log = LoggerFactory.getLogger(SyncMonitoringController.class);

    private final SyncMonitoringService monitoringService;

    public SyncMonitoringController(SyncMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/runs")
    /**
     * Returns the most recent sync runs (limited by the provided parameter).
     */
    public List<SyncRunResponse> listRuns(@RequestParam(name = "limit", defaultValue = "10") int limit) {
        log.info("Fetching last {} sync runs", limit);
        List<SyncRunResponse> responses = monitoringService.findRecentRuns(limit);
        log.info("Returning {} sync runs", responses.size());
        return responses;
    }

    @GetMapping("/state")
    /**
     * Returns the latest known last-successful-sync timestamps per logical table.
     */
    public List<SyncStateResponse> listStates() {
        log.info("Fetching current sync states");
        List<SyncStateResponse> responses = monitoringService.fetchSyncStates();
        log.info("Returning {} sync states", responses.size());
        return responses;
    }
}

