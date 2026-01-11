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
    List<ChartOfAccount> findAllByAccountGroupId(long accountGroupId);
    Optional<ChartOfAccount> findByCodeAndOrganizationId(String code, long organizationId);
    boolean existsByCodeAndOrganizationId(String code, long organizationId);
    List<ChartOfAccount> findAllByOrganizationIdAndProjectId(long organizationId, Long projectId);
    List<ChartOfAccount> findAllByOrganizationIdAndUnitId(long organizationId, Long unitId);
    List<ChartOfAccount> findAllByOrganizationIdAndCustomerId(long organizationId, Long customerId);
    List<ChartOfAccount> findAllByOrganizationIdAndVendorId(long organizationId, Long vendorId);
    List<ChartOfAccount> findAllByOrganizationIdAndAccountGroup(
            long organizationId, AccountGroup accountGroup);
    List<ChartOfAccount> findAllByOrganizationIdAndAccountGroup_Id(long organizationId, long accountGroupId);
    List<ChartOfAccount> findAllByOrganizationIdAndAccountGroupIn(
            long organizationId, List<AccountGroup> groups);

}



