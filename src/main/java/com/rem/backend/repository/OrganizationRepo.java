package com.rem.backend.repository;

import com.rem.backend.entity.organization.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepo extends JpaRepository<Organization , Long> {

    Optional<Organization> findByOrganizationIdAndIsActiveTrue(long id);

}
