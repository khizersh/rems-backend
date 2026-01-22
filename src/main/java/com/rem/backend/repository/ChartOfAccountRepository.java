package com.rem.backend.repository;

import com.rem.backend.entity.account.AccountGroup;
import com.rem.backend.entity.account.ChartOfAccount;
import com.rem.backend.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, Long> {
    List<ChartOfAccount> findAllByOrganization_OrganizationId(long organizationId);
    List<ChartOfAccount> findAllByOrganization_OrganizationIdAndStatus(long organizationId, AccountStatus status);
    boolean existsByCodeAndOrganization_OrganizationId(String code, long organizationId);
    List<ChartOfAccount> findAllByOrganization_OrganizationIdAndAccountGroup(
            long organizationId, AccountGroup accountGroup);
    List<ChartOfAccount> findAllByOrganization_OrganizationIdAndAccountGroup_Id(long organizationId, long accountGroupId);
    List<ChartOfAccount> findAllByOrganization_OrganizationIdAndAccountGroupIn(
            long organizationId, List<AccountGroup> groups);
    Optional<ChartOfAccount> findByOrganizationAccountIdAndStatus(
            Long organizationAccountId,
            AccountStatus status
    );
    boolean existsByNameAndOrganization_OrganizationId(String name, long organizationId);

    boolean existsByNameAndOrganization_OrganizationIdAndIdNot(
            String name,
            long organizationId,
            long id
    );
}



