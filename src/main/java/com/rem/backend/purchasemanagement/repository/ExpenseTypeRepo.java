package com.rem.backend.purchasemanagement.repository;

import com.rem.backend.entity.expense.ExpenseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExpenseTypeRepo extends JpaRepository<ExpenseType, Long> {

   boolean  existsByNameContainingIgnoreCaseAndOrganizationId(String name, long orgId);
   List<ExpenseType>  findAllByOrganizationId(long orgId);
   Page<ExpenseType> findAllByOrganizationId(long organizationId, Pageable pageable);
}
