package com.rem.backend.usermanagement.repository;

import com.rem.backend.usermanagement.entity.CustomRoleMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CustomRoleMappingRepo extends JpaRepository<CustomRoleMapping , Long> {

    Set<CustomRoleMapping> findByUserId(long userId);

    Set<CustomRoleMapping> findByRoleId(long roleId);

    Set<CustomRoleMapping> findByRoleIdAndUserId(long roleId, long userId);

    boolean existsByRoleIdAndUserId(long roleId, long userId);

    Set<CustomRoleMapping> findByUserIdAndIsActiveTrue(long userId);

    Set<CustomRoleMapping> findByRoleIdAndIsActiveTrue(long roleId);

}
