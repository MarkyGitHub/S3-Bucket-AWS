package com.contargo.s3sync.customer;

/**
 * Read-only service for retrieving customers.
 */
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Returns all customers from the repository.
     */
    public List<Customer> findAll() {
        log.debug("Fetching all customers from repository");
        List<Customer> customers = customerRepository.findAll();
        log.info("Fetched {} customers", customers.size());
        return customers;
    }
}

