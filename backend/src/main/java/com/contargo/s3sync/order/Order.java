package com.contargo.s3sync.order;

/**
 * JPA entity representing an order with auditing timestamps.
 */
import com.contargo.s3sync.customer.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "auftraege")
public class Order {

    @Id
    @Column(name = "auftragid")
    private String id;

    @Column(name = "artikelnummer", nullable = false)
    private String articleNumber;

    @Column(name = "created", nullable = false)
    private OffsetDateTime created;

    @Column(name = "lastchange", nullable = false)
    private OffsetDateTime lastChange;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kundeid", nullable = false)
    private Customer customer;

    @PrePersist
    /**
     * Initializes auditing timestamps on first persist.
     */
    void onCreate() {
        if (created == null) {
            created = OffsetDateTime.now();
        }
        if (lastChange == null) {
            lastChange = OffsetDateTime.now();
        }
    }

    @PreUpdate
    /**
     * Advances the lastChange timestamp on update.
     */
    void onUpdate() {
        lastChange = OffsetDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getArticleNumber() {
        return articleNumber;
    }

    public void setArticleNumber(String articleNumber) {
        this.articleNumber = articleNumber;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }

    public OffsetDateTime getLastChange() {
        return lastChange;
    }

    public void setLastChange(OffsetDateTime lastChange) {
        this.lastChange = lastChange;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
}

