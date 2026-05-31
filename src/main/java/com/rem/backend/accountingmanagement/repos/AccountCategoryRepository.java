package com.rem.backend.accountingmanagement.repos;

import com.rem.backend.accountingmanagement.entity.AccountCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountCategoryRepository extends JpaRepository<AccountCategory, Long> {

    List<AccountCategory> findAllByOrganization_OrganizationId(long organizationId);

    List<AccountCategory> findAllByAccountType_IdAndOrganization_OrganizationId(long accountTypeId, long organizationId);

    Optional<AccountCategory> findByNameAndOrganization_OrganizationId(String name, long organizationId);

    boolean existsByNameAndOrganization_OrganizationId(String name, Long organizationId);

    boolean existsByNameAndOrganization_OrganizationIdAndIdNot(String name, Long organizationId, Long id);
}

