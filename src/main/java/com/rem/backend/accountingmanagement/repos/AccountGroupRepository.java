package com.rem.backend.accountingmanagement.repos;

import com.rem.backend.accountingmanagement.entity.AccountCategory;
import com.rem.backend.accountingmanagement.entity.AccountGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountGroupRepository extends JpaRepository<AccountGroup, Long> {

    List<AccountGroup> findAllByOrganization_OrganizationId(long organizationId);

    List<AccountGroup> findAllByAccountCategory_IdAndOrganization_OrganizationId(long accountCategoryId, long organizationId);

    List<AccountGroup> findAllByAccountCategory_Id(long accountCategoryId);

    Optional<AccountGroup> findByNameAndOrganization_OrganizationId(
            String name, long organizationId);

    List<AccountGroup> findAllByOrganization_OrganizationIdAndAccountCategory(
            long organizationId, AccountCategory accountCategory);

    List<AccountGroup> findAllByOrganization_OrganizationIdAndAccountCategory_Id(
            long organizationId, long accountCategoryId);

    boolean existsByNameAndOrganization_OrganizationId(String name, Long organizationId);

    boolean existsByNameAndOrganization_OrganizationIdAndIdNot(
            String name, Long organizationId, Long id);

}
