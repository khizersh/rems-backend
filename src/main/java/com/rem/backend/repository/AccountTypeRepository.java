package com.rem.backend.repository;

import com.rem.backend.entity.account.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountTypeRepository extends JpaRepository<AccountType, Long> {
    Optional<AccountType> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}


