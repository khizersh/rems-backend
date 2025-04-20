package com.rem.backend.repository;

import com.rem.backend.entity.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ProjectRepo extends JpaRepository<Project , Long> {
    Optional<Project> findByProjectIdAndIsActiveTrue(long id);

    Page<Project> findByOrganizationId(long organizationId, Pageable pageable);

    @Query(value = "SELECT p.name FROM project p WHERE p.project_id = :projectId", nativeQuery = true)
    String findProjectNameById(long projectId);


    @Query(value = "SELECT p.project_id as id, p.name as name FROM project p WHERE p.organization_id = :organizationId", nativeQuery = true)
    List<Map<String , Object>> findAllByOrganizationId(long organizationId);
}
