package com.contargo.s3sync.customer;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {

    List<Customer> findByUpdatedAtAfter(OffsetDateTime updatedAt);

    List<Customer> findByCountry(String country);
}

