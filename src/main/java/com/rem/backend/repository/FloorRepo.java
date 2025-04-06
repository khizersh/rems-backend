package com.rem.backend.repository;

import com.rem.backend.entity.project.Floor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FloorRepo extends JpaRepository<Floor , Long> {
}
