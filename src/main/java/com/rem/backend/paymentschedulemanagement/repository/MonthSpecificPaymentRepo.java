package com.rem.backend.paymentschedulemanagement.repository;

import com.rem.backend.paymentschedulemanagement.entity.MonthSpecificPayment;
import com.rem.backend.paymentschedulemanagement.entity.MonthWisePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonthSpecificPaymentRepo extends JpaRepository<MonthSpecificPayment, Long> {


    List<MonthSpecificPayment> findByPaymentScheduleId(long paymentId);
}
