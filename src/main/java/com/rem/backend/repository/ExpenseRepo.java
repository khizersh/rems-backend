package com.rem.backend.repository;

import com.rem.backend.entity.expense.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepo extends JpaRepository<Expense, Long> {

    Page<Expense> findAllByOrganizationId(long orgId , Pageable pageable);

    Page<Expense> findAllByVendorAccountId(long vendorAccountId, Pageable pageable);

    Page<Expense> findAllByProjectId(long projectId, Pageable pageable);


    Page<Expense> findAllByProjectIdAndVendorAccountId(long projectId, long vendorAccountId, Pageable pageable);



}
