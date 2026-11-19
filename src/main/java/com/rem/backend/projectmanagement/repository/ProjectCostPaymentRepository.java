package com.rem.backend.projectmanagement.repository;

import com.rem.backend.projectmanagement.entity.ProjectCostPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProjectCostPaymentRepository extends JpaRepository<ProjectCostPayment, Long> {

    List<ProjectCostPayment> findByProjectCostIdOrderByPaymentDateDescIdDesc(Long projectCostId);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM ProjectCostPayment p
            WHERE p.projectCostId = :projectCostId AND p.reversed = false
            """)
    BigDecimal sumSuccessfulPayments(@Param("projectCostId") Long projectCostId);

    Optional<ProjectCostPayment> findByIdAndOrganizationId(Long id, Long organizationId);

    boolean existsByProjectCostIdAndReversedFalse(Long projectCostId);
}
