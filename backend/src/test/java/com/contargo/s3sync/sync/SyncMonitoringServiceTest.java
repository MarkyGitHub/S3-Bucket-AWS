package com.contargo.s3sync.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.contargo.s3sync.sync.api.SyncRunResponse;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncMonitoringServiceTest {

    @Mock
    private SyncRunRepository syncRunRepository;

    @Mock
    private SyncStateRepository syncStateRepository;

    @InjectMocks
    private SyncMonitoringService syncMonitoringService;

    @Test
    void findRecentRuns_returnsMappedDtos() {
        SyncRun run = new SyncRun();
        run.setStatus(SyncStatus.SUCCESS);
        run.setStartedAt(OffsetDateTime.now().minusHours(1));
        run.setFinishedAt(OffsetDateTime.now());
        run.addItem(new SyncRunItem("kunde", "DE", 2, "kunde/kunde_DE.csv"));
        when(syncRunRepository.findTop20ByOrderByStartedAtDesc()).thenReturn(List.of(run));

        List<SyncRunResponse> result = syncMonitoringService.findRecentRuns(5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).items()).hasSize(1);
    }
}

