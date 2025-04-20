package com.rem.backend.repository;

import com.rem.backend.entity.paymentschedule.MonthWisePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MonthWisePaymentRepo extends JpaRepository<MonthWisePayment , Long> {
}
