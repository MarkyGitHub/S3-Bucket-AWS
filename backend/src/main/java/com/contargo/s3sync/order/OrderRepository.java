package com.contargo.s3sync.order;

/**
 * Repository for CRUD and custom operations on {@link Order}.
 */
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, String> {

    /**
     * Finds orders changed after the given timestamp.
     */
    List<Order> findByLastChangeAfter(OffsetDateTime lastChange);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value
            = """
            WITH selected_auftraege AS (
                SELECT auftragid
                FROM (
                    SELECT
                        auftragid,
                        kundeid,
                        ROW_NUMBER() OVER (PARTITION BY kundeid ORDER BY lastchange DESC) AS rn
                    FROM auftraege
                ) ranked
                WHERE rn <= 2
            )
            UPDATE auftraege a
            SET lastchange = :timestamp
            FROM selected_auftraege sa
            WHERE a.auftragid = sa.auftragid
            """,
            nativeQuery = true)
    /**
     * Updates lastChange for the two most recent orders per customer to the given timestamp.
     *
     * @return number of rows updated
     */
    int updateLastChangeForTopTwoPerCustomer(@Param("timestamp") OffsetDateTime timestamp);
}
