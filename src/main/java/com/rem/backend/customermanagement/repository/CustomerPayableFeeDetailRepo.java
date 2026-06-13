package com.rem.backend.customermanagement.repository;

import com.rem.backend.customermanagement.entity.CustomerPayableFeeDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerPayableFeeDetailRepo extends JpaRepository<CustomerPayableFeeDetail, Long> {
}
