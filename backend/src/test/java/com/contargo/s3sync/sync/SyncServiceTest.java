package com.contargo.s3sync.sync;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.contargo.s3sync.customer.Customer;
import com.contargo.s3sync.customer.CustomerRepository;
import com.contargo.s3sync.order.Order;
import com.contargo.s3sync.order.OrderRepository;
import com.contargo.s3sync.s3.S3Service;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SyncStateRepository syncStateRepository;

    @Mock
    private SyncRunRepository syncRunRepository;

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private SyncService syncService;

    private Customer customerDe;
    private Customer customerFr;
    private Order orderDe;
    private Order orderFr;

    @BeforeEach
    @SuppressWarnings({"unused", "null"})
    void setup() {
        customerDe = buildCustomer("1", "DE", OffsetDateTime.now().minusDays(1));
        customerFr = buildCustomer("2", "FR", OffsetDateTime.now().minusHours(10));

        orderDe = buildOrder("A-1", customerDe, OffsetDateTime.now().minusHours(5));
        orderFr = buildOrder("A-2", customerFr, OffsetDateTime.now().minusHours(2));

        when(syncRunRepository.save(any(SyncRun.class))).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(s3Service.isBucketEmpty()).thenReturn(false);
    }

    @Test
    void runSync_exportsCustomersAndOrdersGroupedByCountry() {
        when(syncStateRepository.findById("kunde")).thenReturn(Optional.empty());
        when(syncStateRepository.findById("auftraege")).thenReturn(Optional.empty());
        when(customerRepository.findAll()).thenReturn(List.of(customerDe, customerFr));
        when(orderRepository.findAll()).thenReturn(List.of(orderDe, orderFr));

        SyncRun run = syncService.runSync();

        verify(s3StorageService, times(2)).store(eq("kunde"), any(), any(), any());
        verify(s3StorageService, times(2)).store(eq("auftraege"), any(), any(), any());

        assertThat(run.getStatus()).isEqualTo(SyncStatus.SUCCESS);
    }

    @Test
    void runSync_groupsCustomersByCountryAndCreatesSeparateCsvPerCountry() {
        Customer anotherDe = buildCustomer("3", "DE", OffsetDateTime.now().minusHours(6));
        Customer anotherFr = buildCustomer("4", "FR", OffsetDateTime.now().minusHours(4));

        List<Customer> deCustomers = List.of(customerDe, anotherDe);
        List<Customer> frCustomers = List.of(customerFr, anotherFr);

        when(syncStateRepository.findById("kunde")).thenReturn(Optional.empty());
        when(syncStateRepository.findById("auftraege")).thenReturn(Optional.empty());
        when(customerRepository.findAll()).thenReturn(List.of(customerDe, anotherDe, customerFr, anotherFr));
        when(orderRepository.findAll()).thenReturn(List.of());

        syncService.runSync();

        ArgumentCaptor<String> countryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);

        verify(s3StorageService, times(2)).store(eq("kunde"), countryCaptor.capture(), any(), contentCaptor.capture());

        Map<String, String> csvByCountry = new HashMap<>();
        List<String> countries = countryCaptor.getAllValues();
        List<String> contents = contentCaptor.getAllValues();
        for (int i = 0; i < countries.size(); i++) {
            csvByCountry.put(countries.get(i), contents.get(i));
        }

        assertThat(csvByCountry.keySet()).containsExactlyInAnyOrder("DE", "FR");

        String deCsv = csvByCountry.get("DE");
        assertThat(deCsv).isNotNull();
        assertThat(deCsv.split("\n")).hasSize(deCustomers.size());
        deCustomers.forEach(customer -> assertThat(deCsv).contains(customer.getId()));
        frCustomers.forEach(customer -> assertThat(deCsv).doesNotContain(customer.getId()));

        String frCsv = csvByCountry.get("FR");
        assertThat(frCsv).isNotNull();
        assertThat(frCsv.split("\n")).hasSize(frCustomers.size());
        frCustomers.forEach(customer -> assertThat(frCsv).contains(customer.getId()));
        deCustomers.forEach(customer -> assertThat(frCsv).doesNotContain(customer.getId()));
    }

    @Test
    void runSync_writesCustomerCsvWithExpectedColumnOrderAndNoHeader() {
        when(syncStateRepository.findById("kunde")).thenReturn(Optional.empty());
        when(syncStateRepository.findById("auftraege")).thenReturn(Optional.empty());
        when(customerRepository.findAll()).thenReturn(List.of(customerDe));
        when(orderRepository.findAll()).thenReturn(List.of());

        syncService.runSync();

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(s3StorageService).store(eq("kunde"), eq(customerDe.getCountry()), any(), contentCaptor.capture());
        verify(s3StorageService, never()).store(eq("auftraege"), any(), any(), any());

        String csv = contentCaptor.getValue();
        assertThat(csv.split("\n")).hasSize(1);

        String expected = String.join(",",
                customerDe.getCompanyName(),
                customerDe.getStreet(),
                customerDe.getStreetExtra(),
                customerDe.getCity(),
                customerDe.getCountry(),
                customerDe.getPostalCode(),
                customerDe.getFirstName(),
                customerDe.getLastName(),
                customerDe.getId()
        );

        assertThat(csv).isEqualTo(expected);
        assertThat(csv.split(",")).hasSize(9);
    }

    @Test
    void runSync_groupsOrdersByCustomerCountryAndCreatesSeparateCsvPerCountry() {
        Order anotherOrderDe = buildOrder("A-3", customerDe, OffsetDateTime.now().minusHours(1));
        Order anotherOrderFr = buildOrder("A-4", customerFr, OffsetDateTime.now().minusMinutes(30));

        List<Order> deOrders = List.of(orderDe, anotherOrderDe);
        List<Order> frOrders = List.of(orderFr, anotherOrderFr);

        when(syncStateRepository.findById("kunde")).thenReturn(Optional.empty());
        when(syncStateRepository.findById("auftraege")).thenReturn(Optional.empty());
        when(customerRepository.findAll()).thenReturn(List.of());
        when(orderRepository.findAll()).thenReturn(List.of(orderDe, anotherOrderDe, orderFr, anotherOrderFr));

        syncService.runSync();

        verify(s3StorageService, never()).store(eq("kunde"), any(), any(), any());

        ArgumentCaptor<String> countryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);

        verify(s3StorageService, times(2)).store(eq("auftraege"), countryCaptor.capture(), any(), contentCaptor.capture());

        Map<String, String> csvByCountry = new HashMap<>();
        List<String> countries = countryCaptor.getAllValues();
        List<String> contents = contentCaptor.getAllValues();
        for (int i = 0; i < countries.size(); i++) {
            csvByCountry.put(countries.get(i), contents.get(i));
        }

        assertThat(csvByCountry.keySet()).containsExactlyInAnyOrder("DE", "FR");

        String deCsv = csvByCountry.get("DE");
        assertThat(deCsv.split("\n")).hasSize(deOrders.size());
        deOrders.forEach(order -> assertThat(deCsv).contains(order.getId()));
        frOrders.forEach(order -> assertThat(deCsv).doesNotContain(order.getId()));

        String frCsv = csvByCountry.get("FR");
        assertThat(frCsv.split("\n")).hasSize(frOrders.size());
        frOrders.forEach(order -> assertThat(frCsv).contains(order.getId()));
        deOrders.forEach(order -> assertThat(frCsv).doesNotContain(order.getId()));
    }

    @Test
    void runSync_writesOrderCsvWithExpectedColumnOrderAndNoHeader() {
        when(syncStateRepository.findById("kunde")).thenReturn(Optional.empty());
        when(syncStateRepository.findById("auftraege")).thenReturn(Optional.empty());
        when(customerRepository.findAll()).thenReturn(List.of());
        when(orderRepository.findAll()).thenReturn(List.of(orderDe));

        syncService.runSync();

        verify(s3StorageService, never()).store(eq("kunde"), any(), any(), any());

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(s3StorageService).store(eq("auftraege"), eq(orderDe.getCustomer().getCountry()), any(), contentCaptor.capture());

        String csv = contentCaptor.getValue();
        assertThat(csv.split("\n")).hasSize(1);

        String expected = String.join(",",
                orderDe.getId(),
                orderDe.getArticleNumber(),
                orderDe.getCustomer().getId()
        );

        assertThat(csv).isEqualTo(expected);
        assertThat(csv.split(",")).hasSize(3);
    }

    @Test
    void runSync_recordsCompletionTimestamp() {
        when(syncStateRepository.findById("kunde")).thenReturn(Optional.empty());
        when(syncStateRepository.findById("auftraege")).thenReturn(Optional.empty());
        when(customerRepository.findAll()).thenReturn(List.of(customerDe));
        when(orderRepository.findAll()).thenReturn(List.of(orderDe));

        SyncRun run = syncService.runSync();

        assertThat(run.getFinishedAt()).isNotNull();
        assertThat(run.getFinishedAt()).isAfterOrEqualTo(run.getStartedAt());
    }

    @Test
    void runSync_forcesFullExportWhenBucketIsEmpty() {
        when(s3Service.isBucketEmpty()).thenReturn(true);
        OffsetDateTime lastSync = OffsetDateTime.now().minusDays(2);
        when(syncStateRepository.findById("kunde")).thenReturn(Optional.of(new SyncState("kunde", lastSync)));
        when(syncStateRepository.findById("auftraege")).thenReturn(Optional.of(new SyncState("auftraege", lastSync)));
        when(customerRepository.findAll()).thenReturn(List.of(customerDe));
        when(orderRepository.findAll()).thenReturn(List.of(orderDe));

        syncService.runSync();

        verify(customerRepository).findAll();
        verify(customerRepository, never()).findByUpdatedAtAfter(any(OffsetDateTime.class));
        verify(orderRepository).findAll();
        verify(orderRepository, never()).findByLastChangeAfter(any(OffsetDateTime.class));
    }

    @Test
    @SuppressWarnings({"NullAway", "null"})
    void runSync_updatesSyncStateWithLatestTimestampsPerTable() {
        OffsetDateTime lastCustomerSync = OffsetDateTime.now().minusDays(5).withNano(0);
        OffsetDateTime lastOrderSync = OffsetDateTime.now().minusDays(6).withNano(0);
        OffsetDateTime latestCustomerUpdate = lastCustomerSync.plusHours(12);
        OffsetDateTime latestOrderChange = lastOrderSync.plusHours(8);

        customerDe.setCountry("DE");
        customerDe.setUpdatedAt(latestCustomerUpdate.minusHours(2));
        customerFr.setCountry("FR");
        customerFr.setUpdatedAt(latestCustomerUpdate);

        orderDe.setCustomer(customerDe);
        orderDe.setLastChange(latestOrderChange.minusHours(1));
        orderFr.setCustomer(customerFr);
        orderFr.setLastChange(latestOrderChange);

        when(syncStateRepository.findById("kunde")).thenReturn(Optional.of(new SyncState("kunde", lastCustomerSync)));
        when(syncStateRepository.findById("auftraege")).thenReturn(Optional.of(new SyncState("auftraege", lastOrderSync)));
        when(customerRepository.findByUpdatedAtAfter(lastCustomerSync)).thenReturn(List.of(customerDe, customerFr));
        when(orderRepository.findByLastChangeAfter(lastOrderSync)).thenReturn(List.of(orderDe, orderFr));

        syncService.runSync();

        ArgumentCaptor<SyncState> stateCaptor = ArgumentCaptor.forClass(SyncState.class);
        verify(syncStateRepository, times(2)).save(stateCaptor.capture());

        List<SyncState> savedStates = stateCaptor.getAllValues();
        assertThat(savedStates).hasSize(2);

        SyncState customerState = savedStates.stream()
                .filter(state -> "kunde".equals(state.getTableName()))
                .findFirst()
                .orElseThrow();
        SyncState orderState = savedStates.stream()
                .filter(state -> "auftraege".equals(state.getTableName()))
                .findFirst()
                .orElseThrow();

        assertThat(customerState.getLastSuccessfulSync()).isEqualTo(customerFr.getUpdatedAt());
        assertThat(orderState.getLastSuccessfulSync()).isEqualTo(orderFr.getLastChange());
    }

    @Test
    void runSync_filtersDataUsingLastSuccessfulSyncTimestamps() {
        OffsetDateTime lastSync = OffsetDateTime.now().minusDays(3);

        when(syncStateRepository.findById("kunde")).thenReturn(Optional.of(new SyncState("kunde", lastSync)));
        when(syncStateRepository.findById("auftraege")).thenReturn(Optional.of(new SyncState("auftraege", lastSync)));
        when(customerRepository.findByUpdatedAtAfter(lastSync)).thenReturn(List.of(customerDe));
        when(orderRepository.findByLastChangeAfter(lastSync)).thenReturn(List.of(orderDe));

        syncService.runSync();

        verify(customerRepository).findByUpdatedAtAfter(lastSync);
        verify(customerRepository, never()).findAll();
        verify(orderRepository).findByLastChangeAfter(lastSync);
        verify(orderRepository, never()).findAll();
    }

    @Test
    void runSync_exportsNewAndModifiedRecords() {
        OffsetDateTime lastCustomerSync = OffsetDateTime.now().minusDays(7).withNano(0);
        OffsetDateTime lastOrderSync = OffsetDateTime.now().minusDays(7).withNano(0);

        Customer newCustomer = buildCustomer("3", "DE", lastCustomerSync.plusHours(1));
        Customer modifiedCustomer = buildCustomer("4", "FR", lastCustomerSync.plusHours(3));
        Order newOrder = buildOrder("B-1", newCustomer, lastOrderSync.plusHours(2));
        Order modifiedOrder = buildOrder("B-2", modifiedCustomer, lastOrderSync.plusHours(4));

        when(syncStateRepository.findById("kunde")).thenReturn(Optional.of(new SyncState("kunde", lastCustomerSync)));
        when(syncStateRepository.findById("auftraege")).thenReturn(Optional.of(new SyncState("auftraege", lastOrderSync)));
        when(customerRepository.findByUpdatedAtAfter(lastCustomerSync)).thenReturn(List.of(newCustomer, modifiedCustomer));
        when(orderRepository.findByLastChangeAfter(lastOrderSync)).thenReturn(List.of(newOrder, modifiedOrder));

        SyncRun run = syncService.runSync();

        ArgumentCaptor<String> tableCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> countryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OffsetDateTime> timestampCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);

        verify(s3StorageService, times(4))
                .store(tableCaptor.capture(), countryCaptor.capture(), timestampCaptor.capture(), contentCaptor.capture());

        assertThat(tableCaptor.getAllValues()).containsExactlyInAnyOrder("kunde", "kunde", "auftraege", "auftraege");
        assertThat(countryCaptor.getAllValues()).containsExactlyInAnyOrder("DE", "FR", "DE", "FR");
        assertThat(contentCaptor.getAllValues())
                .anySatisfy(content -> assertThat(content).contains(newCustomer.getId()))
                .anySatisfy(content -> assertThat(content).contains(modifiedCustomer.getId()))
                .anySatisfy(content -> assertThat(content).contains(newOrder.getId()))
                .anySatisfy(content -> assertThat(content).contains(modifiedOrder.getId()));

        assertThat(run.getItems())
                .extracting(SyncRunItem::getTableName, SyncRunItem::getCountry, SyncRunItem::getObjectCount)
                .containsExactlyInAnyOrder(
                        tuple("kunde", "DE", 1),
                        tuple("kunde", "FR", 1),
                        tuple("auftraege", "DE", 1),
                        tuple("auftraege", "FR", 1)
                );
    }

    @Test
    void runSync_skipsExportWhenNoChangesDetected() {
        OffsetDateTime lastSync = OffsetDateTime.now().minusHours(12);

        when(syncStateRepository.findById("kunde")).thenReturn(Optional.of(new SyncState("kunde", lastSync)));
        when(syncStateRepository.findById("auftraege")).thenReturn(Optional.of(new SyncState("auftraege", lastSync)));
        when(customerRepository.findByUpdatedAtAfter(lastSync)).thenReturn(List.of());
        when(orderRepository.findByLastChangeAfter(lastSync)).thenReturn(List.of());

        SyncRun run = syncService.runSync();

        verify(s3StorageService, never()).store(any(), any(), any(), any());
        assertThat(run.getItems()).isEmpty();
    }

    private Customer buildCustomer(String id, String country, OffsetDateTime updatedAt) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setCountry(country);
        customer.setFirstName("First" + id);
        customer.setLastName("Last" + id);
        customer.setCompanyName("Company" + id);
        customer.setStreet("Street" + id);
        customer.setStreetExtra("StreetExtra" + id);
        customer.setCity("City" + id);
        customer.setPostalCode("0000" + id);
        customer.setUpdatedAt(updatedAt);
        return customer;
    }

    private Order buildOrder(String id, Customer customer, OffsetDateTime lastChange) {
        Order order = new Order();
        order.setId(id);
        order.setArticleNumber("ART" + id);
        order.setCustomer(customer);
        order.setLastChange(lastChange);
        order.setCreated(lastChange.minusDays(1));
        return order;
    }
}
