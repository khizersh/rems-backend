package com.rem.backend.repository;

import com.rem.backend.entity.customerpayable.CustomerPayableFeeDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerPayableFeeDetailRepo extends JpaRepository<CustomerPayableFeeDetail, Long> {
}
