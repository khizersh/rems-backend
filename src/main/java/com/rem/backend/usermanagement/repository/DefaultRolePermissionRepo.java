package com.rem.backend.usermanagement.repository;

import com.rem.backend.usermanagement.entity.DefaultRolePermissionMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface DefaultRolePermissionRepo extends JpaRepository<DefaultRolePermissionMapping, Long> {

    Set<DefaultRolePermissionMapping> findByRoleId(long roleId);

}
