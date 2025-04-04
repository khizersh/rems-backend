package com.rem.backend.usermanagement.repository;

import com.rem.backend.usermanagement.entity.UserRoleMapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface UserRoleMappingRepository extends JpaRepository<UserRoleMapper, Long> {

    Set<UserRoleMapper> findByRoleCode(String role);
}
