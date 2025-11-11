package com.contargo.s3sync.customer;

/**
 * Repository for working with {@link Customer} entities.
 */
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {

    /** Finds customers updated after the given timestamp. */
    List<Customer> findByUpdatedAtAfter(OffsetDateTime updatedAt);

    /** Finds customers by ISO country code. */
    List<Customer> findByCountry(String country);
}

