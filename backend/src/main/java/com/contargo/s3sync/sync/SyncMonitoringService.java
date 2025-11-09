package com.contargo.s3sync.sync;

import com.contargo.s3sync.sync.api.SyncRunItemResponse;
import com.contargo.s3sync.sync.api.SyncRunResponse;
import com.contargo.s3sync.sync.api.SyncStateResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SyncMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(SyncMonitoringService.class);

    private final SyncRunRepository syncRunRepository;
    private final SyncStateRepository syncStateRepository;

    public SyncMonitoringService(SyncRunRepository syncRunRepository, SyncStateRepository syncStateRepository) {
        this.syncRunRepository = syncRunRepository;
        this.syncStateRepository = syncStateRepository;
    }

    public List<SyncRunResponse> findRecentRuns(int limit) {
        log.debug("Fetching up to {} recent sync runs", limit);
        List<SyncRunResponse> responses = syncRunRepository.findTop20ByOrderByStartedAtDesc().stream()
            .limit(limit)
            .map(run -> new SyncRunResponse(
                run.getId(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getStatus().name(),
                run.getErrorMessage(),
                run.getItems().stream()
                    .map(item -> new SyncRunItemResponse(
                        item.getTableName(),
                        item.getCountry(),
                        item.getObjectCount(),
                        item.getS3Key()))
                    .collect(Collectors.toList())
            ))
            .collect(Collectors.toList());
        log.info("Prepared {} sync run responses", responses.size());
        return responses;
    }

    public List<SyncStateResponse> fetchSyncStates() {
        log.debug("Fetching sync state overview");
        List<SyncStateResponse> responses = syncStateRepository.findAll().stream()
            .map(state -> new SyncStateResponse(state.getTableName(), state.getLastSuccessfulSync()))
            .collect(Collectors.toList());
        log.info("Prepared {} sync state responses", responses.size());
        return responses;
    }
}

