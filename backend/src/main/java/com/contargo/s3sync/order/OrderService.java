package com.contargo.s3sync.order;

import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<OrderSummaryDto> findAllSummaries() {
        log.debug("Fetching all orders from repository");
        List<OrderSummaryDto> summaries = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "lastChange")).stream()
                .map(order -> new OrderSummaryDto(
                        order.getId(),
                        order.getArticleNumber(),
                        order.getCreated(),
                        order.getLastChange(),
                        order.getCustomer() != null ? order.getCustomer().getId() : null))
                .toList();
        log.info("Fetched {} orders", summaries.size());
        return summaries;
    }

    @Transactional
    public OrderUpdateResult updateLastChangeForTopTwoOrdersPerCustomer() {
        OffsetDateTime timestamp = OffsetDateTime.now();
        int updatedRows = orderRepository.updateLastChangeForTopTwoPerCustomer(timestamp);
        log.info(
                "Updated lastChange timestamp for {} orders across customers using timestamp {}",
                updatedRows,
                timestamp);
        return new OrderUpdateResult(updatedRows, timestamp);
    }

    public record OrderSummaryDto(
            String id,
            String articleNumber,
            OffsetDateTime created,
            OffsetDateTime lastChange,
            String customerId) {
    }

    public record OrderUpdateResult(int updatedRows, OffsetDateTime appliedTimestamp) {}
}

