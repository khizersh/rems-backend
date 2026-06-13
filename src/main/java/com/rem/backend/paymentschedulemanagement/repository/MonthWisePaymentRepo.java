package com.rem.backend.paymentschedulemanagement.repository;

import com.rem.backend.paymentschedulemanagement.entity.MonthWisePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MonthWisePaymentRepo extends JpaRepository<MonthWisePayment , Long> {


    List<MonthWisePayment> findByPaymentScheduleId(long paymentId);
}
