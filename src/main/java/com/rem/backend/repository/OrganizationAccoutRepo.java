package com.rem.backend.repository;

import com.rem.backend.entity.organization.OrganizationAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrganizationAccoutRepo extends JpaRepository<OrganizationAccount , Long> {

    List<OrganizationAccount> findByOrganizationId(long orgId);
}
