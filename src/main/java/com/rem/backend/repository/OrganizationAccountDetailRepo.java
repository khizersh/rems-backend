package com.rem.backend.repository;

import com.rem.backend.entity.project.Project;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.rem.backend.entity.organizationAccount.OrganizationAccountDetail;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationAccountDetailRepo extends JpaRepository<OrganizationAccountDetail, Long > {

    Page<OrganizationAccountDetail> findByOrganizationAcctId(long orgAcctId , Pageable pageable);
    Optional<OrganizationAccountDetail> findByExpenseId(long expenseId);
    @Transactional
    void deleteByExpenseId(Long expenseId);
}
