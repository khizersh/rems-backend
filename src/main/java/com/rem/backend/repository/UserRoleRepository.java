package com.rem.backend.repository;

import com.rem.backend.usermanagement.entity.UserRoles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRoles, Long> {

    public Set<UserRoles> findByUserId(long userId);
}


