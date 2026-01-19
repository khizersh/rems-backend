package com.rem.backend.repository;

import com.rem.backend.entity.account.AccountGroup;
import com.rem.backend.entity.account.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountGroupRepository extends JpaRepository<AccountGroup, Long> {

    List<AccountGroup> findAllByOrganization_OrganizationId(long organizationId);

    List<AccountGroup> findAllByAccountType_IdAndOrganization_OrganizationId(long accountType,long organizationId);

    List<AccountGroup> findAllByAccountType_Id(long accountTypeId);

    Optional<AccountGroup> findByNameAndOrganization_OrganizationId(
            String name, long organizationId);

    List<AccountGroup> findAllByOrganization_OrganizationIdAndAccountType(
            long organizationId, AccountType accountType);

    // OR (recommended)
    List<AccountGroup> findAllByOrganization_OrganizationIdAndAccountType_Id(
            long organizationId, long accountTypeId);

    boolean existsByNameAndOrganization_OrganizationId(String name, Long organizationId);

}
