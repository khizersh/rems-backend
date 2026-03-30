package com.rem.backend.warehousemanagement.repo;

import com.rem.backend.warehousemanagement.entity.Warehouse;
import com.rem.backend.enums.WarehouseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    List<Warehouse> findByOrganizationIdAndActiveTrue(Long organizationId);

    Page<Warehouse> findByOrganizationIdAndActiveTrue(Long organizationId, Pageable pageable);

    List<Warehouse> findByProjectIdAndActiveTrue(Long projectId);

    Page<Warehouse> findByProjectIdAndActiveTrue(Long projectId, Pageable pageable);

    Optional<Warehouse> findByCodeAndOrganizationId(String code, Long organizationId);

    List<Warehouse> findByWarehouseTypeAndOrganizationIdAndActiveTrue(WarehouseType warehouseType, Long organizationId);

    boolean existsByCodeAndOrganizationId(String code, Long organizationId);

    @Query("SELECT w FROM Warehouse w WHERE w.organizationId = :organizationId AND w.warehouseType = :warehouseType AND w.active = true")
    List<Warehouse> findByOrganizationAndType(@Param("organizationId") Long organizationId,
                                             @Param("warehouseType") WarehouseType warehouseType);
}
