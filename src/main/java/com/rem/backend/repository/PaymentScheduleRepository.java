package com.rem.backend.repository;

import com.rem.backend.entity.paymentschedule.PaymentSchedule;
import com.rem.backend.enums.PaymentScheduleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule , Long> {

    Optional<PaymentSchedule> findByUnitIdAndPaymentScheduleType(Long unitId , PaymentScheduleType paymentScheduleType);
}
