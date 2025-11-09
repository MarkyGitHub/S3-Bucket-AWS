package com.contargo.s3sync.sync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sync_run_item")
public class SyncRunItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private SyncRun run;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "object_count", nullable = false)
    private int objectCount;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    public SyncRunItem() {
    }

    public SyncRunItem(String tableName, String country, int objectCount, String s3Key) {
        this.tableName = tableName;
        this.country = country;
        this.objectCount = objectCount;
        this.s3Key = s3Key;
    }

    public Long getId() {
        return id;
    }

    public SyncRun getRun() {
        return run;
    }

    void setRun(SyncRun run) {
        this.run = run;
    }

    public String getTableName() {
        return tableName;
    }

    public String getCountry() {
        return country;
    }

    public int getObjectCount() {
        return objectCount;
    }

    public String getS3Key() {
        return s3Key;
    }
}

