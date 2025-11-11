package com.contargo.s3sync.sync;

/**
 * Repository for persisting and querying {@link SyncRun} entities.
 */
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncRunRepository extends JpaRepository<SyncRun, Long> {

    /**
     * Returns the most recent runs ordered by start time (descending).
     */
    java.util.List<SyncRun> findTop20ByOrderByStartedAtDesc();
}

