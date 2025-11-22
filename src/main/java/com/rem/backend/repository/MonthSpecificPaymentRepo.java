package com.rem.backend.repository;

import com.rem.backend.entity.paymentschedule.MonthSpecificPayment;
import com.rem.backend.entity.paymentschedule.MonthWisePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonthSpecificPaymentRepo extends JpaRepository<MonthSpecificPayment, Long> {


    List<MonthSpecificPayment> findByPaymentScheduleId(long paymentId);
}
