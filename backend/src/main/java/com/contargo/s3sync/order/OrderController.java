package com.contargo.s3sync.order;

/**
 * Demo endpoints for listing orders and touching their lastChange timestamp.
 */
import com.contargo.s3sync.order.OrderService.OrderSummaryDto;
import com.contargo.s3sync.order.OrderService.OrderUpdateResult;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    /**
     * Returns a summarized list of orders sorted by lastChange descending.
     */
    public List<OrderSummaryDto> listOrders() {
        log.info("Received request to list orders");
        return orderService.findAllSummaries();
    }

    @PostMapping("/lastchange/touch")
    /**
     * Updates lastChange for the two most recent orders per customer.
     */
    public OrderUpdateResponse touchLastChangeForOrders() {
        log.info("Received request to update lastChange for top two orders per customer");
        OrderUpdateResult result = orderService.updateLastChangeForTopTwoOrdersPerCustomer();
        return new OrderUpdateResponse(result.updatedRows(), result.appliedTimestamp());
    }

    public record OrderUpdateResponse(int updatedRows, OffsetDateTime appliedTimestamp) {

    }
}
