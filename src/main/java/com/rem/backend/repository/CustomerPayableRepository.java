package com.rem.backend.repository;

import com.rem.backend.entity.customerpayable.CustomerPayable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerPayableRepository extends JpaRepository<CustomerPayable, Long> {
}
