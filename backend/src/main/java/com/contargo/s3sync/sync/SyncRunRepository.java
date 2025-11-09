package com.contargo.s3sync.sync;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncRunRepository extends JpaRepository<SyncRun, Long> {

    java.util.List<SyncRun> findTop20ByOrderByStartedAtDesc();
}

