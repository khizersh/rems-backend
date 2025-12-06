package com.rem.backend.repository;

import com.rem.backend.entity.paymentschedule.PaymentSchedule;
import com.rem.backend.enums.PaymentScheduleType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule , Long> {

    Optional<PaymentSchedule> findByUnitIdAndPaymentScheduleTypeAndIsActiveTrue(Long unitId , PaymentScheduleType paymentScheduleType);


    @Query(value = """
        SELECT ps 
        FROM PaymentSchedule ps 
        WHERE ps.unit.id = (
            SELECT ca.unit.id 
            FROM CustomerAccount ca 
            WHERE ca.id = :customerAccountId
        )
        AND ps.paymentScheduleType = :paymentScheduleType
        AND ps.isActive = true
    """)
    PaymentSchedule findByCustomerAccountIdAndPaymentScheduleType(
            @Param("customerAccountId") Long customerAccountId,
            @Param("paymentScheduleType") PaymentScheduleType paymentScheduleType
    );

    void deleteByUnit_Id(long id);
}
