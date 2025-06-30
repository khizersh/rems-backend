package com.rem.backend.repository;

import com.rem.backend.entity.expense.ExpenseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExpenseTypeRepo extends JpaRepository<ExpenseType, Long> {

   boolean  existsByNameContainingIgnoreCaseAndOrganizationId(String name, long orgId);
   List<ExpenseType>  findAllByOrganizationId(long orgId);
}
