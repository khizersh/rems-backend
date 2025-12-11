package com.rem.backend.repository;

import com.rem.backend.entity.customerpayable.CustomerPayable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerPayableRepository extends JpaRepository<CustomerPayable, Long> {

    @Query("""
            SELECT cp
            FROM CustomerPayable cp
            JOIN FETCH cp.details d
            WHERE cp.booking.id = :bookingId
            AND cp.unit.id = :unitId
            """)
    Optional<CustomerPayable> findWithDetails(long bookingId, long unitId);

}
