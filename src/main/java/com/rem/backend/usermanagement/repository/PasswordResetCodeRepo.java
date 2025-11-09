package com.rem.backend.usermanagement.repository;

import com.rem.backend.usermanagement.entity.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetCodeRepo extends JpaRepository<PasswordResetCode, Long> {

    Optional<PasswordResetCode> findByEmailAndCodeAndIsExpiredFalse(String email, String code);
    Optional<PasswordResetCode> findByEmailAndIsExpiredFalse(String email);

    @Query("SELECT p FROM PasswordResetCode p " +
            "WHERE p.code = :code " +
            "AND p.isExpired = false " +
            "AND p.createdDate >= :validFrom")
    Optional<PasswordResetCode> findValidCode(String code, LocalDateTime validFrom);
}
