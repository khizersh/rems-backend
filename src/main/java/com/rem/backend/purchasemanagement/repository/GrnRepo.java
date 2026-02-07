package com.rem.backend.purchasemanagement.repository;

import com.rem.backend.purchasemanagement.entity.grn.Grn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GrnRepo extends JpaRepository<Grn, Long> {

    List<Grn> findByPoId(Long poId);

    Optional<Grn> findTopByOrderByIdDesc();

    Page<Grn> findByPoId(Long poId, Pageable pageable);
}
