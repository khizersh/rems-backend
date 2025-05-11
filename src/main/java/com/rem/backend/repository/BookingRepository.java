package com.rem.backend.repository;

import com.rem.backend.entity.booking.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByUnit_Id(Long unitId);

    Page<Booking> findByOrganizationId(Long projectId, Pageable pageable);
    Page<Booking> findByProjectId(Long projectId, Pageable pageable);
    Page<Booking> findByFloorId(Long projectId, Pageable pageable);

    Page<Booking> findByUnitId(Long unitId, Pageable pageable);

}
