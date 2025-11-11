package com.contargo.s3sync.sync;

/**
 * REST endpoint to trigger a sync run on demand.
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private static final Logger log = LoggerFactory.getLogger(SyncController.class);

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/run")
    /**
     * Triggers an asynchronous sync run and returns the accepted {@link SyncRun}.
     */
    public ResponseEntity<SyncRun> triggerSync() {
        log.info("Received request to trigger sync run");
        SyncRun run = syncService.runSync();
        log.info("Sync run {} accepted with status {}", run.getId(), run.getStatus());
        return ResponseEntity.accepted().body(run);
    }
}

