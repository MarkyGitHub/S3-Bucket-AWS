package com.contargo.s3sync.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contargo.s3sync.customer.Customer;
import com.contargo.s3sync.customer.CustomerRepository;
import com.contargo.s3sync.order.Order;
import com.contargo.s3sync.order.OrderRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Captor
    private ArgumentCaptor<SyncRun> syncRunCaptor;

    @InjectMocks
    private SyncService syncService;

    private Customer customerDe;
    private Customer customerFr;
    private Order orderDe;
    private Order orderFr;

    @BeforeEach
    void setup() {
        customerDe = buildCustomer("1", "DE", OffsetDateTime.now().minusDays(1));
        customerFr = buildCustomer("2", "FR", OffsetDateTime.now().minusHours(10));

        orderDe = buildOrder("A-1", customerDe, OffsetDateTime.now().minusHours(5));
        orderFr = buildOrder("A-2", customerFr, OffsetDateTime.now().minusHours(2));

        when(syncRunRepository.save(any(SyncRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(s3StorageService.store(any(), any(), any(), any())).thenReturn("dummy-key");
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

    private Customer buildCustomer(String id, String country, OffsetDateTime updatedAt) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setCountry(country);
        customer.setFirstName("First" + id);
        customer.setLastName("Last" + id);
        customer.setCompanyName("Company" + id);
        customer.setStreet("Street" + id);
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

