package com.rem.backend.usermanagement.repository;

import com.rem.backend.usermanagement.entity.Permissions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepo extends JpaRepository<Permissions , Long> {

    boolean existsByCode(String code);

    boolean existsByEndPoint(String endPoint);

    Optional<Permissions> findByCode(String code);

    Optional<Permissions> findByEndPoint(String endPoint);
}
