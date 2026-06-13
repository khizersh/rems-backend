package com.rem.backend.customermanagement.repository;

import com.rem.backend.customermanagement.entity.CustomerPayable;
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

    Optional<CustomerPayable> findByBooking_IdAndUnit_Id(Long bookingId, Long unitId);


}
