package com.rem.backend.sidebarmanagement.repository;

import com.rem.backend.sidebarmanagement.entity.GrandChildSidebar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GrandChildSidebarRepository extends JpaRepository<GrandChildSidebar, Long> {
}
