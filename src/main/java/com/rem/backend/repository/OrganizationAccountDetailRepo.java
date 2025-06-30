package com.rem.backend.repository;

import com.rem.backend.entity.project.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.rem.backend.entity.organizationAccount.OrganizationAccountDetail;
import java.util.List;

@Repository
public interface OrganizationAccountDetailRepo extends JpaRepository<OrganizationAccountDetail, Long > {

    Page<OrganizationAccountDetail> findByOrganizationAcctId(long orgAcctId , Pageable pageable);
}
