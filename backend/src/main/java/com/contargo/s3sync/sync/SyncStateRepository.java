package com.contargo.s3sync.sync;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncStateRepository extends JpaRepository<SyncState, String> {

    Optional<SyncState> findByTableName(String tableName);
}

