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
    List<ChartOfAccount> findAllByOrganizationId(long organizationId);
    List<ChartOfAccount> findAllByOrganizationIdAndStatus(long organizationId, AccountStatus status);
    boolean existsByCodeAndOrganizationId(String code, long organizationId);
    List<ChartOfAccount> findAllByOrganizationIdAndAccountGroup(
            long organizationId, AccountGroup accountGroup);
    List<ChartOfAccount> findAllByOrganizationIdAndAccountGroup_Id(long organizationId, long accountGroupId);
    List<ChartOfAccount> findAllByOrganizationIdAndAccountGroupIn(
            long organizationId, List<AccountGroup> groups);

}



