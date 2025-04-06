package com.rem.backend.repository;

import com.rem.backend.entity.project.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApartmentRepo extends JpaRepository<Apartment , Long> {
}
