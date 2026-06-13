package com.rem.backend.customermanagement.repository;

import com.rem.backend.customermanagement.entity.CustomerPayableDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerPayableDetailRepository extends JpaRepository<CustomerPayableDetail, Long> {
}
