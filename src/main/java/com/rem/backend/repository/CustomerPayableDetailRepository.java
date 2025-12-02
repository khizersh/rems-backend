package com.rem.backend.repository;

import com.rem.backend.entity.customerpayable.CustomerPayableDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerPayableDetailRepository extends JpaRepository<CustomerPayableDetail, Long> {
}
