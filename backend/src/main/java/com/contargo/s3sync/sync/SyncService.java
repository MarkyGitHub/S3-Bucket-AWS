package com.contargo.s3sync.sync;

import com.contargo.s3sync.customer.Customer;
import com.contargo.s3sync.customer.CustomerRepository;
import com.contargo.s3sync.order.Order;
import com.contargo.s3sync.order.OrderRepository;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final SyncStateRepository syncStateRepository;
    private final SyncRunRepository syncRunRepository;
    private final S3StorageService s3StorageService;

    public SyncService(CustomerRepository customerRepository, OrderRepository orderRepository,
        SyncStateRepository syncStateRepository, SyncRunRepository syncRunRepository,
        S3StorageService s3StorageService) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.syncStateRepository = syncStateRepository;
        this.syncRunRepository = syncRunRepository;
        this.s3StorageService = s3StorageService;
    }

    @Transactional
    public SyncRun runSync() {
        OffsetDateTime startedAt = OffsetDateTime.now(ZoneOffset.UTC);
        SyncRun run = new SyncRun();
        run.setStartedAt(startedAt);
        run.setStatus(SyncStatus.RUNNING);
        run = syncRunRepository.save(run);

        try {
            log.info("Starting sync run {}", run.getId());
            processCustomers(run, startedAt);
            processOrders(run, startedAt);

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

    private void processCustomers(SyncRun run, OffsetDateTime startedAt) {
        OffsetDateTime since = syncStateRepository.findById("kunde")
            .map(SyncState::getLastSuccessfulSync)
            .orElse(OffsetDateTime.MIN);

        List<Customer> customers = since.equals(OffsetDateTime.MIN)
            ? customerRepository.findAll()
            : customerRepository.findByUpdatedAtAfter(since);

        if (customers.isEmpty()) {
            log.info("No customer updates detected since {}", since);
            updateSyncState("kunde", startedAt);
            return;
        }

        log.info("Processing {} customer updates since {}", customers.size(), since);
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

    private void processOrders(SyncRun run, OffsetDateTime startedAt) {
        OffsetDateTime since = syncStateRepository.findById("auftraege")
            .map(SyncState::getLastSuccessfulSync)
            .orElse(OffsetDateTime.MIN);

        List<Order> orders = since.equals(OffsetDateTime.MIN)
            ? orderRepository.findAll()
            : orderRepository.findByLastChangeAfter(since);

        if (orders.isEmpty()) {
            log.info("No order updates detected since {}", since);
            updateSyncState("auftraege", startedAt);
            return;
        }

        log.info("Processing {} order updates since {}", orders.size(), since);
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

    private void updateSyncState(String tableName, OffsetDateTime lastSync) {
        syncStateRepository.save(new SyncState(tableName, lastSync));
    }

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

    private String orderToCsv(Order order) {
        return String.join(",",
            safe(order.getId()),
            safe(order.getArticleNumber()),
            safe(order.getCustomer().getId())
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }
}

