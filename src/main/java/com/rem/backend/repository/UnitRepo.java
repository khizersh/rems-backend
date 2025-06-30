package com.rem.backend.repository;

import com.rem.backend.entity.project.Floor;
import com.rem.backend.entity.project.Unit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface UnitRepo extends JpaRepository<Unit, Long> {


    Page<Unit> findByFloorId(long floorId, Pageable pageable);
    List<Unit> findByFloorId(long floorId);

    int countByFloorId(Long floorId);

    @Query(value = "SELECT u.serial_no FROM unit u WHERE u.id = :id" , nativeQuery = true)
    String findUnitSerialById(long id);

    @Query(value = "SELECT id as id, serial_no as serialNo  FROM unit  WHERE floor_id = :floorId And is_booked = 0 ;", nativeQuery = true)
    List<Map<String , Object>> findAllUnitByFloorIdAndIsBookedFalse(long floorId);




}
