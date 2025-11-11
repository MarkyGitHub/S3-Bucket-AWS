package com.contargo.s3sync.sync;

/**
 * Orchestrates synchronization of domain data to S3.
 * Determines the effective change window, exports customers and orders,
 * persists run state, and updates last successful sync timestamps.
 */
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.contargo.s3sync.customer.Customer;
import com.contargo.s3sync.customer.CustomerRepository;
import com.contargo.s3sync.order.Order;
import com.contargo.s3sync.order.OrderRepository;
import com.contargo.s3sync.s3.S3Service;

import jakarta.transaction.Transactional;

@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final SyncStateRepository syncStateRepository;
    private final SyncRunRepository syncRunRepository;
    private final S3StorageService s3StorageService;
    private final S3Service s3Service;

    public SyncService(CustomerRepository customerRepository, OrderRepository orderRepository,
        SyncStateRepository syncStateRepository, SyncRunRepository syncRunRepository,
        S3StorageService s3StorageService, S3Service s3Service) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.syncStateRepository = syncStateRepository;
        this.syncRunRepository = syncRunRepository;
        this.s3StorageService = s3StorageService;
        this.s3Service = s3Service;
    }

    @Transactional
    /**
     * Executes a single sync run: exports changed data, writes to S3, and records status.
     *
     * @return the persisted {@link SyncRun} with collected item batches
     */
    public SyncRun runSync() {
        OffsetDateTime startedAt = OffsetDateTime.now(ZoneOffset.UTC);
        SyncRun run = new SyncRun();
        run.setStartedAt(startedAt);
        run.setStatus(SyncStatus.RUNNING);
        run = syncRunRepository.save(run);

        try {
            boolean forceFullSync = shouldForceFullSync();
            if (forceFullSync) {
                log.info("Detected empty S3 bucket; forcing full data export for this run");
            }

            log.info("Starting sync run {}", run.getId());
            processCustomers(run, startedAt, forceFullSync);
            processOrders(run, startedAt, forceFullSync);

            run.setStatus(SyncStatus.SUCCESS);
            log.info("Sync run {} completed successfully with {} items", run.getId(), run.getItems().size());
            return run;
        } catch (Exception e) {
            log.error("Sync run {} failed", run.getId(), e);
            run.setStatus(SyncStatus.FAILED);
            run.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            run.setFinishedAt(OffsetDateTime.now(ZoneOffset.UTC));
            syncRunRepository.save(run);
        }
    }

    /**
     * Processes customer changes and stores country-partitioned CSV files to S3.
     *
     * @param run the current sync run
     * @param startedAt the run start timestamp
     * @param forceFullSync when true, exports all data regardless of stored state
     */
    private void processCustomers(SyncRun run, OffsetDateTime startedAt, boolean forceFullSync) {
        OffsetDateTime persistedSince = syncStateRepository.findById("kunde")
            .map(SyncState::getLastSuccessfulSync)
            .orElse(OffsetDateTime.MIN);
        OffsetDateTime effectiveSince = forceFullSync ? OffsetDateTime.MIN : persistedSince;

        if (forceFullSync) {
            if (persistedSince.equals(OffsetDateTime.MIN)) {
                log.info("Executing first-time full customer export");
            } else {
                log.info("Ignoring stored customer sync timestamp {} because S3 bucket is empty", persistedSince);
            }
        }

        List<Customer> customers = effectiveSince.equals(OffsetDateTime.MIN)
            ? customerRepository.findAll()
            : customerRepository.findByUpdatedAtAfter(effectiveSince);

        if (customers.isEmpty()) {
            if (forceFullSync) {
                log.info("No customer records available to export during forced full sync");
            } else {
                log.info("No customer updates detected since {}", effectiveSince);
            }
            updateSyncState("kunde", startedAt);
            return;
        }

        log.info("Processing {} customer updates since {}", customers.size(),
            effectiveSince.equals(OffsetDateTime.MIN) ? "the beginning" : effectiveSince);
        Map<String, List<Customer>> customersByCountry = customers.stream()
            .collect(Collectors.groupingBy(Customer::getCountry));

        OffsetDateTime latestUpdate = customers.stream()
            .map(Customer::getUpdatedAt)
            .filter(date -> date != null)
            .max(OffsetDateTime::compareTo)
            .orElse(startedAt);

        customersByCountry.forEach((country, group) -> {
            String csv = group.stream()
                .map(this::customerToCsv)
                .collect(Collectors.joining("\n"));
            String key = s3StorageService.store("kunde", country, startedAt, csv);
            run.addItem(new SyncRunItem("kunde", country, group.size(), key));
        });

        syncRunRepository.save(run);
        updateSyncState("kunde", latestUpdate);
    }

    /**
     * Processes order changes and stores country-partitioned CSV files to S3.
     *
     * @param run the current sync run
     * @param startedAt the run start timestamp
     * @param forceFullSync when true, exports all data regardless of stored state
     */
    private void processOrders(SyncRun run, OffsetDateTime startedAt, boolean forceFullSync) {
        OffsetDateTime persistedSince = syncStateRepository.findById("auftraege")
            .map(SyncState::getLastSuccessfulSync)
            .orElse(OffsetDateTime.MIN);
        OffsetDateTime effectiveSince = forceFullSync ? OffsetDateTime.MIN : persistedSince;

        if (forceFullSync) {
            if (persistedSince.equals(OffsetDateTime.MIN)) {
                log.info("Executing first-time full order export");
            } else {
                log.info("Ignoring stored order sync timestamp {} because S3 bucket is empty", persistedSince);
            }
        }

        List<Order> orders = effectiveSince.equals(OffsetDateTime.MIN)
            ? orderRepository.findAll()
            : orderRepository.findByLastChangeAfter(effectiveSince);

        if (orders.isEmpty()) {
            if (forceFullSync) {
                log.info("No order records available to export during forced full sync");
            } else {
                log.info("No order updates detected since {}", effectiveSince);
            }
            updateSyncState("auftraege", startedAt);
            return;
        }

        log.info("Processing {} order updates since {}", orders.size(),
            effectiveSince.equals(OffsetDateTime.MIN) ? "the beginning" : effectiveSince);
        Map<String, List<Order>> ordersByCountry = orders.stream()
            .collect(Collectors.groupingBy(order -> order.getCustomer().getCountry()));

        OffsetDateTime latestUpdate = orders.stream()
            .map(Order::getLastChange)
            .max(OffsetDateTime::compareTo)
            .orElse(startedAt);

        ordersByCountry.forEach((country, group) -> {
            String csv = group.stream()
                .map(this::orderToCsv)
                .collect(Collectors.joining("\n"));
            String key = s3StorageService.store("auftraege", country, startedAt, csv);
            run.addItem(new SyncRunItem("auftraege", country, group.size(), key));
        });

        syncRunRepository.save(run);
        updateSyncState("auftraege", latestUpdate);
    }

    /**
     * Persists the last successful sync timestamp for a given logical table.
     */
    private void updateSyncState(String tableName, OffsetDateTime lastSync) {
        syncStateRepository.save(new SyncState(tableName, lastSync));
    }

    /**
     * Serializes a customer into a simple comma-separated line.
     */
    private String customerToCsv(Customer customer) {
        return String.join(",",
            safe(customer.getCompanyName()),
            safe(customer.getStreet()),
            safe(customer.getStreetExtra()),
            safe(customer.getCity()),
            safe(customer.getCountry()),
            safe(customer.getPostalCode()),
            safe(customer.getFirstName()),
            safe(customer.getLastName()),
            safe(customer.getId())
        );
    }

    /**
     * Serializes an order into a simple comma-separated line.
     */
    private String orderToCsv(Order order) {
        return String.join(",",
            safe(order.getId()),
            safe(order.getArticleNumber()),
            safe(order.getCustomer().getId())
        );
    }

    /**
     * Returns the input with commas replaced and null mapped to empty string.
     */
    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    /**
     * Determines whether a full sync should be forced based on S3 bucket state.
     */
    private boolean shouldForceFullSync() {
        return s3Service.isBucketEmpty();
    }
}

