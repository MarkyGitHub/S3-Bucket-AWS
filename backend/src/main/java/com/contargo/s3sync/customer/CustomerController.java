package com.contargo.s3sync.customer;

/**
 * Demo endpoint for listing customers.
 */
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    /**
     * Returns all customers.
     */
    public List<Customer> listCustomers() {
        log.info("Received request to list customers");
        return customerService.findAll();
    }
}

