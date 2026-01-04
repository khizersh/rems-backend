package com.rem.backend.repository;

import com.rem.backend.entity.account.AccountGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountGroupRepository extends JpaRepository<AccountGroup, Long> {
    List<AccountGroup> findAllByOrganizationId(long organizationId);
    List<AccountGroup> findAllByAccountTypeId(long accountTypeId);
    List<AccountGroup> findAllByOrganizationIdAndAccountTypeId(long organizationId, long accountTypeId);
    boolean existsByNameContainingIgnoreCaseAndOrganizationId(String name, long organizationId);
}


