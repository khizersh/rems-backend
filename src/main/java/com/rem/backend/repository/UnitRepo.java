package com.rem.backend.repository;

import com.rem.backend.entity.project.Floor;
import com.rem.backend.entity.project.Unit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UnitRepo extends JpaRepository<Unit, Long> {


    Page<Unit> findByFloorId(long floorId, Pageable pageable);

    int countByFloorId(Long floorId);

    @Query(value = "SELECT u.serial_no FROM unit u WHERE u.id = :id" , nativeQuery = true)
    String findUnitSerialById(long id);




}
