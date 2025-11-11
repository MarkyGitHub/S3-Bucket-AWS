package com.contargo.s3sync.sync;

/**
 * Repository for reading and writing {@link SyncState} rows.
 */
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncStateRepository extends JpaRepository<SyncState, String> {

    /**
     * Finds the state row for the given logical table name.
     */
    Optional<SyncState> findByTableName(String tableName);
}

