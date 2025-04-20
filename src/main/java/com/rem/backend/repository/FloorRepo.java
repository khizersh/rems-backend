package com.rem.backend.repository;

import com.rem.backend.entity.project.Floor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface FloorRepo extends JpaRepository<Floor , Long> {

    Page<Floor> findByProjectId(long projectId, Pageable pageable);

    @Query(value = "SELECT f.floor FROM floor f WHERE f.id = :id" , nativeQuery = true)
    String findFloorNoById(long id);


    @Query(value = "SELECT id as id, f.floor as floorNo  FROM floor f WHERE f.project_id = :projectId", nativeQuery = true)
    List<Map<String , Object>> findAllFloorByProjectId(long projectId);
}
