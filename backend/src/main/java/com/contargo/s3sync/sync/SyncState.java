package com.contargo.s3sync.sync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "sync_state")
public class SyncState {

    @Id
    @Column(name = "table_name")
    private String tableName;

    @Column(name = "last_successful_sync")
    private OffsetDateTime lastSuccessfulSync;

    public SyncState() {
    }

    public SyncState(String tableName, OffsetDateTime lastSuccessfulSync) {
        this.tableName = tableName;
        this.lastSuccessfulSync = lastSuccessfulSync;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public OffsetDateTime getLastSuccessfulSync() {
        return lastSuccessfulSync;
    }

    public void setLastSuccessfulSync(OffsetDateTime lastSuccessfulSync) {
        this.lastSuccessfulSync = lastSuccessfulSync;
    }
}

