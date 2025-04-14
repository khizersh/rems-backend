package com.rem.backend.repository;

import com.rem.backend.entity.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepo extends JpaRepository<Project , Long> {
    Optional<Project> findByProjectIdAndIsActiveTrue(long id);
    List<Project> findByOrganizationId(long id);

    Page<Project> findByOrganizationId(long organizationId, Pageable pageable);
}
