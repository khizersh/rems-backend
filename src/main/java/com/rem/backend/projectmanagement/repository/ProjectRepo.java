package com.rem.backend.projectmanagement.repository;

import com.rem.backend.projectmanagement.entity.Project;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ProjectRepo extends JpaRepository<Project , Long> {
    Optional<Project> findByProjectIdAndIsActiveTrue(long id);
    Optional<Project> findByProjectIdAndOrganizationIdAndIsActiveTrue(long projectId, long organizationId);
    boolean existsByPropertyPurchaseIdAndIsActiveTrue(Long propertyPurchaseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.projectId = :projectId AND p.organizationId = :organizationId AND p.isActive = true")
    Optional<Project> findActiveForUpdate(@Param("projectId") long projectId, @Param("organizationId") long organizationId);

    Page<Project> findByOrganizationId(long organizationId, Pageable pageable);

    @Query(value = "SELECT p.name FROM project p WHERE p.project_id = :projectId", nativeQuery = true)
    String findProjectNameById(long projectId);


    @Query(value = "SELECT p.project_id as id, p.name as name FROM project p WHERE p.organization_id = :organizationId", nativeQuery = true)
    List<Map<String , Object>> findAllByOrganizationId(long organizationId);


    // Count projects created in the last 30 days
    long countByCreatedDateAfterAndOrganizationId(LocalDateTime date, long orgId);
    long countByOrganizationId(long orgId);
}
