package com.rem.backend.repository;

import com.rem.backend.entity.paymentschedule.PaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule , Long> {
}
